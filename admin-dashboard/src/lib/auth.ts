import {
  signInWithEmailAndPassword,
  signOut as firebaseSignOut,
  onAuthStateChanged as firebaseOnAuthStateChanged,
  User,
  NextOrObserver,
} from 'firebase/auth';
import { doc, getDoc } from 'firebase/firestore';
import { auth, db } from './firebase';
import { FIREBASE_COLLECTIONS } from './firebaseAdmin';

export async function signInWithEmail(email: string, password: string) {
  const credential = await signInWithEmailAndPassword(auth, email, password);
  const adminDoc = await getDoc(doc(db, FIREBASE_COLLECTIONS.adminUsers, credential.user.uid));
  if (!adminDoc.exists()) {
    await firebaseSignOut(auth);
    throw new Error('Access denied. Admin privileges required.');
  }
  const adminData = adminDoc.data();
  if (!adminData.isActive) {
    await firebaseSignOut(auth);
    throw new Error('Your account has been deactivated. Contact super admin.');
  }
  return credential.user;
}

export async function signOut() {
  return firebaseSignOut(auth);
}

export function getCurrentUser(): User | null {
  return auth.currentUser;
}

export function onAuthStateChanged(callback: NextOrObserver<User>) {
  return firebaseOnAuthStateChanged(auth, callback);
}

export async function checkIsAdmin(uid: string): Promise<boolean> {
  try {
    const adminDoc = await getDoc(doc(db, FIREBASE_COLLECTIONS.adminUsers, uid));
    return adminDoc.exists() && adminDoc.data()?.isActive === true;
  } catch {
    return false;
  }
}
