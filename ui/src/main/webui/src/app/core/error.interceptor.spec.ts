import { HttpErrorResponse } from '@angular/common/http';
import { serverMessage } from './error.interceptor';

describe('serverMessage', () => {
  const response = (status: number, error: unknown) => new HttpErrorResponse({ status, error });

  it('returns the message of a refused request', () => {
    expect(serverMessage(response(409, { status: 409, error: 'Conflict', message: 'Person is still credited' })))
      .toBe('Person is still credited');
    expect(serverMessage(response(400, { message: 'A person needs a name' }))).toBe('A person needs a name');
  });

  it('returns nothing for other errors or bodies without a message', () => {
    expect(serverMessage(response(500, { message: 'NullPointerException' }))).toBeUndefined();
    expect(serverMessage(response(409, null))).toBeUndefined();
    expect(serverMessage(response(409, { message: '  ' }))).toBeUndefined();
    expect(serverMessage(response(400, 'plain text'))).toBeUndefined();
  });
});
