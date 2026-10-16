import {describe,it,expect} from 'vitest';
import {friendlyErrorMessage} from './authErrors';
describe('friendly authentication errors', () => {
  it('hides the Firebase credential error', () => expect(friendlyErrorMessage({code:'auth/invalid-credential'})).toBe('Incorrect email or password.'));
  it('hides unknown Firebase messages', () => expect(friendlyErrorMessage(new Error('Firebase: Error (auth/internal-error)'))).not.toContain('Firebase'));
  it('retains safe API messages', () => expect(friendlyErrorMessage(new Error('Account disabled'))).toBe('Account disabled'));
});
