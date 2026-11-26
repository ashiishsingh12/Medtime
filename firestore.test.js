const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp(msAgo = 5000) {
  return new Date(Date.now() - msAgo);
}

function futureTimestamp(msAhead = 60000) {
  return new Date(Date.now() + msAhead);
}

function validUserProfileCreate(uid, overrides = {}) {
  const ts = pastTimestamp(10000);
  return {
    userId: uid,
    displayName: "Alice",
    email: "alice@example.com",
    firstName: "Alice",
    morningHour: 8,
    morningMinute: 0,
    afternoonHour: 13,
    afternoonMinute: 30,
    eveningHour: 18,
    eveningMinute: 0,
    nightHour: 21,
    nightMinute: 0,
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

function validFamilyMemberCreate(uid, overrides = {}) {
  const ts = pastTimestamp(10000);
  return {
    userId: uid,
    name: "Self",
    relation: "Self",
    sortOrder: 0,
    defaultMember: true,
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

function validMedicineCreate(uid, overrides = {}) {
  const ts = pastTimestamp(10000);
  return {
    userId: uid,
    memberId: "self",
    name: "Paracetamol",
    dosage: "1 tablet",
    form: "TABLET",
    mealRelation: "AFTER_MEAL",
    notes: "Take with water",
    morningEnabled: true,
    morningHour: 8,
    morningMinute: 0,
    morningMealRelation: "AFTER_MEAL",
    afternoonEnabled: false,
    afternoonHour: 13,
    afternoonMinute: 30,
    afternoonMealRelation: "AFTER_MEAL",
    eveningEnabled: false,
    eveningHour: 18,
    eveningMinute: 0,
    eveningMealRelation: "AFTER_MEAL",
    nightEnabled: false,
    nightHour: 21,
    nightMinute: 0,
    nightMealRelation: "AFTER_MEAL",
    recurrenceFrequency: "DAILY",
    recurrenceInterval: 1,
    recurrenceDays: "",
    recurrenceEndType: "NEVER",
    active: true,
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

function validDoseLogCreate(uid, overrides = {}) {
  const ts = pastTimestamp(10000);
  return {
    userId: uid,
    medicineId: "med_1",
    memberId: "self",
    memberName: "Self",
    medicineName: "Paracetamol",
    dosage: "1 tablet",
    dateKey: "2026-11-25",
    slot: "MORNING",
    status: "TAKEN",
    scheduledTimeLabel: "08:00 AM",
    takenAtEpochMillis: Date.now() - 1000,
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
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

test("Global deny & unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validUserProfileCreate(ALICE_UID))
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("medicines")
      .get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("doseLogs")
      .doc("2026-11-25_MORNING_med_1")
      .set(validDoseLogCreate(ALICE_UID))
  );

  // Unknown collection denied even for signed-in user
  await assertFails(aliceDb.collection("otherCollection").doc("doc_1").get());
  await assertFails(aliceDb.collection("otherCollection").doc("doc_1").set({ foo: "bar" }));
  // Top-level list on users denied
  await assertFails(aliceDb.collection("users").get());
});

test("Owner can create, read, partially update, and delete own UserProfileDoc", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceUserRef = aliceDb.collection("users").doc(ALICE_UID);

  // Valid create matching buildCreateUserProfileMap (omits empty optional keys)
  await assertSucceeds(aliceUserRef.set(validUserProfileCreate(ALICE_UID)));
  await assertSucceeds(aliceUserRef.get());

  // Valid partial update matching updateProfileAndDefaultTimes
  await assertSucceeds(
    aliceUserRef.update({
      displayName: "Alice Sharma",
      email: "alice@example.com",
      firstName: "Alice",
      lastName: "Sharma",
      gender: "Female",
      dob: "01-01-1995",
      state: "Delhi",
      city: "New Delhi",
      phone: "9876543210",
      morningHour: 7,
      morningMinute: 30,
      afternoonHour: 13,
      afternoonMinute: 0,
      eveningHour: 18,
      eveningMinute: 15,
      nightHour: 22,
      nightMinute: 0,
      updatedAt: pastTimestamp(1000),
    })
  );

  // Reject future updatedAt (> request.time)
  await assertFails(
    aliceUserRef.update({
      displayName: "Future Time",
      updatedAt: futureTimestamp(),
    })
  );

  // Reject immutable createdAt or userId change
  await assertFails(
    aliceUserRef.update({
      createdAt: pastTimestamp(500),
      updatedAt: pastTimestamp(500),
    })
  );
  await assertFails(
    aliceUserRef.update({
      userId: BOB_UID,
      updatedAt: pastTimestamp(500),
    })
  );

  // Reject invalid hour (> 23)
  await assertFails(
    aliceUserRef.update({
      morningHour: 25,
      updatedAt: pastTimestamp(500),
    })
  );

  // Reject extra unknown field or @DocumentId 'id' on create
  await assertFails(
    aliceDb
      .collection("users")
      .doc("fresh_doc_invalid")
      .set(validUserProfileCreate("fresh_doc_invalid"))
  );
  await assertSucceeds(aliceUserRef.delete());
});

test("UserProfileDoc rejects extra @DocumentId field and mismatched uid on create", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .set(validUserProfileCreate(ALICE_UID, { id: ALICE_UID }))
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .set(validUserProfileCreate(BOB_UID))
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .set(validUserProfileCreate(ALICE_UID, { createdAt: futureTimestamp() }))
  );
});

test("Owner can CRUD FamilyMembers and list empty or populated subcollection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const membersCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("familyMembers");

  // Empty list succeeds (no existing() in allow list)
  await assertSucceeds(membersCol.orderBy("sortOrder").get());

  // Create default self member + custom member
  await assertSucceeds(
    membersCol.doc("self").set(validFamilyMemberCreate(ALICE_UID))
  );
  await assertSucceeds(
    membersCol
      .doc("mom")
      .set(
        validFamilyMemberCreate(ALICE_UID, {
          name: "Mom",
          relation: "Parent",
          sortOrder: 1,
          defaultMember: true,
        })
      )
  );

  // Matches repository query: orderBy("sortOrder")
  await assertSucceeds(membersCol.orderBy("sortOrder").get());

  // Partial rename update
  await assertSucceeds(
    membersCol.doc("mom").update({
      name: "Maa",
      updatedAt: pastTimestamp(1000),
    })
  );

  // Reject invalid empty name, negative sortOrder, extra id field, immutable createdAt
  await assertFails(
    membersCol.doc("mom").update({
      name: "",
      updatedAt: pastTimestamp(500),
    })
  );
  await assertFails(
    membersCol.doc("mom").update({
      sortOrder: -1,
      updatedAt: pastTimestamp(500),
    })
  );
  await assertFails(
    membersCol.doc("mom").update({
      createdAt: pastTimestamp(500),
      updatedAt: pastTimestamp(500),
    })
  );
  await assertFails(
    membersCol
      .doc("bad_member")
      .set(validFamilyMemberCreate(ALICE_UID, { id: "bad_member" }))
  );

  await assertSucceeds(membersCol.doc("mom").delete());
});

test("Owner can CRUD Medicines, run repository queries, and invalid states are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const medsCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("medicines");

  // Empty list succeeds
  await assertSucceeds(medsCol.get());

  // Valid create (no null optional keys)
  await assertSucceeds(medsCol.doc("med_1").set(validMedicineCreate(ALICE_UID)));

  // Valid create with recurrence dates & count
  await assertSucceeds(
    medsCol.doc("med_2").set(
      validMedicineCreate(ALICE_UID, {
        name: "Vitamin D3",
        recurrenceFrequency: "WEEKLY",
        recurrenceInterval: 1,
        recurrenceDays: "1,4",
        recurrenceEndType: "AFTER_OCCURRENCES",
        recurrenceCount: 10,
        startDateKey: "2026-11-25",
        endDateKey: "2026-12-30",
      })
    )
  );

  // Matches MedicationRepository.deleteFamilyMember query
  await assertSucceeds(medsCol.where("memberId", "==", "self").get());

  // Matches MedicationRepository.toggleMedicineActive update branch
  await assertSucceeds(
    medsCol.doc("med_1").update({
      active: false,
      updatedAt: pastTimestamp(1000),
    })
  );

  // Matches MedicationRepository.updateMedicineRecurrence update branch
  await assertSucceeds(
    medsCol.doc("med_1").update({
      startDateKey: "2026-11-25",
      endDateKey: "2026-12-05",
      recurrenceFrequency: "DAILY",
      recurrenceInterval: 2,
      recurrenceDays: "",
      recurrenceEndType: "ON_DATE",
      recurrenceCount: null,
      active: true,
      updatedAt: pastTimestamp(500),
    })
  );

  // Reject all 4 time slots disabled
  await assertFails(
    medsCol.doc("med_invalid_slots").set(
      validMedicineCreate(ALICE_UID, {
        morningEnabled: false,
        afternoonEnabled: false,
        eveningEnabled: false,
        nightEnabled: false,
      })
    )
  );

  // Reject invalid enum & invalid date format & extra @DocumentId field
  await assertFails(
    medsCol
      .doc("med_invalid_enum")
      .set(validMedicineCreate(ALICE_UID, { mealRelation: "MIDNIGHT" }))
  );
  await assertFails(
    medsCol
      .doc("med_invalid_date")
      .set(validMedicineCreate(ALICE_UID, { startDateKey: "25-11-2026" }))
  );
  await assertFails(
    medsCol
      .doc("med_invalid_id")
      .set(validMedicineCreate(ALICE_UID, { id: "med_invalid_id" }))
  );

  // Reject immutable field modification on update
  await assertFails(
    medsCol.doc("med_1").update({
      userId: BOB_UID,
      updatedAt: pastTimestamp(300),
    })
  );

  await assertSucceeds(medsCol.doc("med_1").delete());
});

test("Owner can CRUD DoseLogs, run memberId query, and invalid states are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const logsCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("doseLogs");
  const logId = "2026-11-25_MORNING_med_1";

  // Empty list succeeds
  await assertSucceeds(logsCol.get());

  // Create DoseLog
  await assertSucceeds(logsCol.doc(logId).set(validDoseLogCreate(ALICE_UID)));

  // Matches MedicationRepository.renameFamilyMember / deleteFamilyMember query
  await assertSucceeds(logsCol.where("memberId", "==", "self").get());

  // Matches batch memberName rename update branch
  await assertSucceeds(
    logsCol.doc(logId).update({
      memberName: "Alice",
      updatedAt: pastTimestamp(1000),
    })
  );

  // Matches markDoseStatus existing-doc update branch
  await assertSucceeds(
    logsCol.doc(logId).update({
      memberName: "Alice",
      medicineName: "Paracetamol",
      dosage: "1 tablet",
      status: "SKIPPED",
      scheduledTimeLabel: "08:00 AM",
      takenAtEpochMillis: Date.now() - 500,
      updatedAt: pastTimestamp(500),
    })
  );

  // Reject invalid slot, status, negative epoch, immutable medicineId change
  await assertFails(
    logsCol
      .doc("2026-11-25_NOON_med_1")
      .set(validDoseLogCreate(ALICE_UID, { slot: "NOON" }))
  );
  await assertFails(
    logsCol
      .doc("2026-11-25_EVENING_med_1")
      .set(validDoseLogCreate(ALICE_UID, { status: "PENDING" }))
  );
  await assertFails(
    logsCol
      .doc("2026-11-25_NIGHT_med_1")
      .set(validDoseLogCreate(ALICE_UID, { takenAtEpochMillis: -10 }))
  );
  await assertFails(
    logsCol.doc(logId).update({
      medicineId: "other_med",
      updatedAt: pastTimestamp(200),
    })
  );

  await assertSucceeds(logsCol.doc(logId).delete());
});

test("Strict cross-user isolation: Bob cannot read, list, create, update, or delete Alice's data", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    const aliceRef = db.collection("users").doc(ALICE_UID);
    await aliceRef.set(validUserProfileCreate(ALICE_UID));
    await aliceRef.collection("familyMembers").doc("self").set(validFamilyMemberCreate(ALICE_UID));
    await aliceRef.collection("medicines").doc("med_1").set(validMedicineCreate(ALICE_UID));
    await aliceRef
      .collection("doseLogs")
      .doc("2026-11-25_MORNING_med_1")
      .set(validDoseLogCreate(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceFromBob = bobDb.collection("users").doc(ALICE_UID);

  // Profile isolation
  await assertFails(aliceFromBob.get());
  await assertFails(aliceFromBob.set(validUserProfileCreate(BOB_UID)));
  await assertFails(
    aliceFromBob.update({ displayName: "Hacked", updatedAt: pastTimestamp(1000) })
  );
  await assertFails(aliceFromBob.delete());

  // FamilyMembers isolation
  await assertFails(aliceFromBob.collection("familyMembers").doc("self").get());
  await assertFails(aliceFromBob.collection("familyMembers").orderBy("sortOrder").get());
  await assertFails(
    aliceFromBob.collection("familyMembers").doc("bob_in_alice").set(validFamilyMemberCreate(BOB_UID))
  );
  await assertFails(
    aliceFromBob.collection("familyMembers").doc("self").update({ name: "Hacked", updatedAt: pastTimestamp(1000) })
  );
  await assertFails(aliceFromBob.collection("familyMembers").doc("self").delete());

  // Medicines isolation
  await assertFails(aliceFromBob.collection("medicines").doc("med_1").get());
  await assertFails(aliceFromBob.collection("medicines").get());
  await assertFails(aliceFromBob.collection("medicines").where("memberId", "==", "self").get());
  await assertFails(
    aliceFromBob.collection("medicines").doc("bob_med").set(validMedicineCreate(BOB_UID))
  );
  await assertFails(
    aliceFromBob.collection("medicines").doc("med_1").update({ active: false, updatedAt: pastTimestamp(1000) })
  );
  await assertFails(aliceFromBob.collection("medicines").doc("med_1").delete());

  // DoseLogs isolation
  await assertFails(aliceFromBob.collection("doseLogs").doc("2026-11-25_MORNING_med_1").get());
  await assertFails(aliceFromBob.collection("doseLogs").get());
  await assertFails(
    aliceFromBob.collection("doseLogs").doc("2026-11-25_NIGHT_med_1").set(validDoseLogCreate(BOB_UID))
  );
  await assertFails(
    aliceFromBob.collection("doseLogs").doc("2026-11-25_MORNING_med_1").update({
      memberName: "Hacked",
      updatedAt: pastTimestamp(1000),
    })
  );
  await assertFails(aliceFromBob.collection("doseLogs").doc("2026-11-25_MORNING_med_1").delete());
});
