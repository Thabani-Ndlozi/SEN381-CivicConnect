import { initializeApp, getApps, type FirebaseApp } from 'firebase/app'
import { getAuth, GoogleAuthProvider, createUserWithEmailAndPassword, signInWithEmailAndPassword,
  signInWithPopup, sendPasswordResetEmail, sendEmailVerification, updateProfile, signOut, onAuthStateChanged, EmailAuthProvider, reauthenticateWithCredential, updatePassword } from 'firebase/auth'

export const firebaseEnabled = import.meta.env.VITE_AUTH_MODE === 'firebase'
let app: FirebaseApp | undefined
if (firebaseEnabled) {
  const apiKey = import.meta.env.VITE_FIREBASE_API_KEY
  const projectId = import.meta.env.VITE_FIREBASE_PROJECT_ID
  const appId = import.meta.env.VITE_FIREBASE_APP_ID
  if (!apiKey || !projectId || !appId) throw new Error('Missing Firebase frontend configuration')
  app = getApps()[0] || initializeApp({apiKey,projectId,appId,
    authDomain:import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || `${projectId}.firebaseapp.com`,
    messagingSenderId:import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID})
}
export const firebaseAuth = app ? getAuth(app) : null
export const getFirebaseToken = async () => {
  if(!firebaseAuth?.currentUser) return null
  return await firebaseAuth.currentUser.getIdToken()
}
export const firebaseEmailLogin = async(email:string,password:string) => {
  if(!firebaseAuth) throw new Error('Firebase not configured')
  return await signInWithEmailAndPassword(firebaseAuth,email,password)
}
export const firebaseEmailSignup = async(email:string,password:string,name:string) => {
  if(!firebaseAuth) throw new Error('Firebase not configured')
  const user = (await createUserWithEmailAndPassword(firebaseAuth,email,password)).user
  if (name) await updateProfile(user,{displayName:name})
  await sendEmailVerification(user)
  await signOut(firebaseAuth)
}
export const firebaseGoogleLogin = async () => {
  if(!firebaseAuth) throw new Error('Firebase not configured')
  return await signInWithPopup(firebaseAuth,new GoogleAuthProvider())
}
/** Send an email-verification link to the currently signed-in Firebase user. */
export const firebaseResendEmailVerification = async () => {
  const user = firebaseAuth?.currentUser
  if (!user) throw new Error('Sign in with your email and password first, then resend verification.')
  await user.reload()
  if (user.emailVerified) throw new Error('This email is already verified. Please sign in again.')
  await sendEmailVerification(user)
}
/** Refresh the Firebase user profile and FORCE a new ID token after verification.
 * Reloading the profile alone does not necessarily update email_verified in a cached JWT. */
export const firebaseRefreshVerifiedSession = async () => {
  const user = firebaseAuth?.currentUser
  if (!user) throw new Error('Please sign in again.')
  await user.reload()
  if (!user.emailVerified) throw new Error('Email address not verified. Use Resend verification email below.')
  await user.getIdToken(true)
}
export const firebaseResetPassword = async(email:string) => {
  if(!firebaseAuth) throw new Error('Firebase not configured')
  await sendPasswordResetEmail(firebaseAuth,email)
}
export const firebaseLogout = async () => {if(firebaseAuth) await signOut(firebaseAuth)}
export const watchFirebaseSession = (callback:(loggedIn:boolean)=>void) => firebaseAuth ?
  onAuthStateChanged(firebaseAuth,user=>callback(Boolean(user?.emailVerified))) : () => {}

/** Require knowledge of the current password to update an email/password Firebase login. */
export const firebaseChangePassword = async (currentPassword: string, newPassword: string) => {
  const user = firebaseAuth?.currentUser;
  if (!user || !user.email) throw new Error('Please sign in again before changing your password.');
  if (newPassword.length < 12) throw new Error('Use at least 12 characters for your new password.');
  await reauthenticateWithCredential(user, EmailAuthProvider.credential(user.email, currentPassword));
  await updatePassword(user, newPassword);
};
