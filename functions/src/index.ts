import {auth} from "firebase-functions/v1";
import {initializeApp} from "firebase-admin/app";
import {getFirestore, FieldValue} from "firebase-admin/firestore";

initializeApp();
const db = getFirestore();

export const createUserDocument = auth.user().onCreate(async (user) => {
  const {uid, email, displayName, photoURL, providerData} = user;

  const userRef = db.collection("users").doc(uid);

  // Guard against re-running on retries/edge cases
  const existing = await userRef.get();
  if (existing.exists) {
    return;
  }

  const authProviders = providerData.map((p) => p.providerId);

  await userRef.set({
    uid,
    email: email || null,
    displayName: displayName || (email ? email.split("@")[0] : "New User"),
    photoUrl: photoURL || null,
    authProviders,
    emailVerified: user.emailVerified,
    createdAt: FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp(),
  });
});

