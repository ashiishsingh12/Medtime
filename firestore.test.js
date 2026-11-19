const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_patient_1";
const BOB_UID = "bob_patient_2";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfile(uid) {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    displayName: "Asha Patient",
    morningHour: 8,
    morningMinute: 0,
    afternoonHour: 13,
    afternoonMinute: 30,
    nightHour: 21,
    nightMinute: 0,
    createdAt: now,
    updatedAt: now,
  };
}

function validMedicine(uid) {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    name: "Metformin 500mg",
    dosage: "1 tablet",
    form: "TABLET",
    mealRelation: "AFTER_MEAL",
    notes: "Take with a glass of water",
    morningEnabled: true,
    morningHour: 8,
    morningMinute: 0,
    afternoonEnabled: false,
    afternoonHour: 13,
    afternoonMinute: 30,
    nightEnabled: true,
    nightHour: 21,
    nightMinute: 0,
    active: true,
    createdAt: now,
    updatedAt: now,
  };
}

function validDoseLog(uid, medicineId) {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    medicineId: medicineId,
    medicineName: "Metformin 500mg",
    dosage: "1 tablet",
    dateKey: "2026-11-19",
    slot: "MORNING",
    status: "TAKEN",
    scheduledTimeLabel: "08:00 AM",
    takenAtEpochMillis: Date.now() - 1000,
    createdAt: now,
    updatedAt: now,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("medicines").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("medicines")
      .doc("med_1")
      .set(validMedicine(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("doseLogs").get()
  );
});

test("Owner: user profile create, partial update, and cross-user denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      morningHour: 7,
      morningMinute: 30,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field & future timestamp & cross-user access
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      morningHour: 9,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
});

test("Owner: medicine create, toggle active, edit, query, and reject invalid payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const medRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("medicines")
    .doc("med_1");

  await assertSucceeds(medRef.set(validMedicine(ALICE_UID)));

  // List query matching Kotlin repository
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("medicines").get()
  );

  // Partial active toggle
  await assertSucceeds(
    medRef.update({
      active: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Full edit update preserving createdAt & userId
  await assertSucceeds(
    medRef.update({
      name: "Metformin 850mg",
      dosage: "1 tablet",
      form: "TABLET",
      mealRelation: "WITH_MEAL",
      notes: "Updated note",
      morningEnabled: true,
      morningHour: 8,
      morningMinute: 30,
      afternoonEnabled: true,
      afternoonHour: 13,
      afternoonMinute: 30,
      nightEnabled: false,
      nightHour: 21,
      nightMinute: 0,
      active: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject @DocumentId 'id' field in payload
  const badWithId = { ...validMedicine(ALICE_UID), id: "med_2" };
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("medicines")
      .doc("med_2")
      .set(badWithId)
  );

  // Reject medicine with zero enabled slots
  const noSlots = {
    ...validMedicine(ALICE_UID),
    morningEnabled: false,
    afternoonEnabled: false,
    nightEnabled: false,
  };
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("medicines")
      .doc("med_3")
      .set(noSlots)
  );

  // Reject createdAt mutation
  await assertFails(
    medRef.update({
      createdAt: new Date(Date.now() - 200),
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Cross-user isolation
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("medicines").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("medicines")
      .doc("med_1")
      .delete()
  );
});

test("Owner: doseLogs create, status update, delete, and dateKey validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const logId = "2026-11-19_MORNING_med_1";
  const logRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("doseLogs")
    .doc(logId);

  await assertSucceeds(logRef.set(validDoseLog(ALICE_UID, "med_1")));

  await assertSucceeds(
    logRef.update({
      status: "SKIPPED",
      takenAtEpochMillis: Date.now() - 500,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable slot mutation
  await assertFails(
    logRef.update({
      slot: "NIGHT",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject malformed dateKey
  const badDate = { ...validDoseLog(ALICE_UID, "med_1"), dateKey: "19-11-2026" };
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("doseLogs")
      .doc("bad_date_log")
      .set(badDate)
  );

  // Cross-user isolation
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("doseLogs")
      .doc(logId)
      .get()
  );

  await assertSucceeds(logRef.delete());
});
