/** Do not expose raw Firebase error codes or backend implementation messages in the UI. */
export function friendlyErrorMessage(error: unknown): string {
  const code = typeof error === 'object' && error !== null && 'code' in error
    ? String((error as {code?: unknown}).code ?? '') : '';
  const messages: Record<string,string> = {
    'auth/invalid-credential': 'Incorrect email or password.',
    'auth/wrong-password': 'Incorrect email or password.',
    'auth/user-not-found': 'Incorrect email or password.',
    'auth/invalid-email': 'Please enter a valid email address.',
    'auth/email-already-in-use': 'An account with this email address already exists. Please sign in instead.',
    'auth/weak-password': 'Choose a stronger password with at least 12 characters.',
    'auth/too-many-requests': 'Too many attempts. Please wait a while before trying again.',
    'auth/network-request-failed': 'We could not connect. Check your internet connection and try again.',
    'auth/popup-closed-by-user': 'Google sign-in was cancelled. Please try again.',
    'auth/cancelled-popup-request': 'Google sign-in was cancelled. Please try again.',
    'auth/popup-blocked': 'Your browser blocked the sign-in popup. Please allow popups and retry.',
    'auth/unauthorized-domain': 'Sign-in is not configured for this website. Contact support.',
    'auth/requires-recent-login': 'For your security, sign out and sign in again before changing your password.',
    'auth/user-disabled': 'This account has been disabled. Contact an administrator.',
    'auth/provider-already-linked': 'This sign-in method is already linked to your account.',
    'auth/operation-not-allowed': 'This sign-in method is not enabled. Contact support.',
  };
  if (messages[code]) return messages[code];
  const raw = error instanceof Error ? error.message : '';
  if (/firebase[: ]|auth\//i.test(raw) || code.startsWith('auth/')) return 'Authentication could not be completed. Please try again or contact support.';
  if (/^Unauthorized$|Invalid credentials/i.test(raw)) return 'Incorrect email or password.';
  return raw || 'Something went wrong. Please try again.';
}
