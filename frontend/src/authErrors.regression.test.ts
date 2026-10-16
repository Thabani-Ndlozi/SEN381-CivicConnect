import { describe, it, expect } from 'vitest';
import { friendlyErrorMessage } from './authErrors';

describe('safe authentication error regression cases', () => {
  it('does not disclose whether email is unregistered', () => {
    expect(friendlyErrorMessage({code:'auth/user-not-found'})).toBe('Incorrect email or password.');
    expect(friendlyErrorMessage({code:'auth/wrong-password'})).toBe('Incorrect email or password.');
  });
  it('gives a useful network outage message', () => {
    expect(friendlyErrorMessage({code:'auth/network-request-failed'})).toMatch(/internet connection/i);
  });
  it('shows a helpful rate-limit message without leaking a Firebase code', () => {
    const msg=friendlyErrorMessage({code:'auth/too-many-requests'});
    expect(msg).toMatch(/Too many attempts/);
    expect(msg).not.toMatch(/auth\//);
  });
  it('handles an email already in use with a nontechnical message', () => {
    expect(friendlyErrorMessage({code:'auth/email-already-in-use'})).toMatch(/already exists/);
  });
  it('converts an invalid email to a corrective prompt', () => {
    expect(friendlyErrorMessage({code:'auth/invalid-email'})).toBe('Please enter a valid email address.');
  });
  it('does not expose an unfamiliar Firebase error code', () => {
    const msg=friendlyErrorMessage({code:'auth/an-unrecognized-code'});
    expect(msg).not.toContain('auth/');
    expect(msg).toMatch(/Authentication could not be completed/);
  });
});
