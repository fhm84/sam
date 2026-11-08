import { HttpInterceptorFn } from '@angular/common/http';

/** Share links' API: open to anyone, no login involved. */
const PUBLIC_API = '/api/public/';

/**
 * Wraps the OIDC interceptor so share links' requests go without the login token. They don't need
 * it, and a token the session still holds but the server no longer accepts (expired, from a
 * removed user) would turn the public page into a 401, shown as "link not found".
 */
export function withoutTokenForPublicApi(auth: HttpInterceptorFn): HttpInterceptorFn {
  return (req, next) => (req.url.startsWith(PUBLIC_API) ? next(req) : auth(req, next));
}
