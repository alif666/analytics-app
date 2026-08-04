This is a [Next.js](https://nextjs.org) project bootstrapped with [`create-next-app`](https://nextjs.org/docs/app/api-reference/cli/create-next-app).

## Getting Started

First, run the development server:

```bash
npm run dev
# or
yarn dev
# or
pnpm dev
# or
bun dev
```

Open [http://localhost:3000](http://localhost:3000) with your browser to see the result.

You can start editing the page by modifying `app/page.tsx`. The page auto-updates as you edit the file.

This project uses [`next/font`](https://nextjs.org/docs/app/building-your-application/optimizing/fonts) to automatically optimize and load [Geist](https://vercel.com/font), a new font family for Vercel.

## Learn More

To learn more about Next.js, take a look at the following resources:

- [Next.js Documentation](https://nextjs.org/docs) - learn about Next.js features and API.
- [Learn Next.js](https://nextjs.org/learn) - an interactive Next.js tutorial.

You can check out [the Next.js GitHub repository](https://github.com/vercel/next.js) - your feedback and contributions are welcome!

## Deploy on Vercel

The easiest way to deploy your Next.js app is to use the [Vercel Platform](https://vercel.com/new?utm_medium=default-template&filter=next.js&utm_source=create-next-app&utm_campaign=create-next-app-readme) from the creators of Next.js.

Check out our [Next.js deployment documentation](https://nextjs.org/docs/app/building-your-application/deploying) for more details.


## Authentication Flow
 Focus on the Next.js side as a client for a Spring Boot auth system. If you understand this layer deeply, you can implement it
  again from scratch and answer interview questions with confidence.

  1. The mental model
  Your Next.js app does not "do authentication" by itself. It does 4 jobs:

  - Collect credentials from the user
  - Send them to the backend
  - Store the session result on the client
  - Protect routes and attach tokens on later requests

  In your current setup, the backend returns:

  - a jwtToken
  - a user object

  The Next app then:

  - stores the JWT in localStorage
  - stores the user in localStorage
  - sends the JWT on later API requests in Authorization
  - sends CSRF token on unsafe requests like POST, PUT, DELETE

  That is the whole flow in one sentence.

  ———

  2. The actual flow in your Next app
  Use these files as the core mental map:

  - /C:/Users/USER/Documents/springboot/analytics/frontend/analytics-app/app/lib/api.ts
  - /C:/Users/USER/Documents/springboot/analytics/frontend/analytics-app/app/context/AuthContext.tsx
  - /C:/Users/USER/Documents/springboot/analytics/frontend/analytics-app/app/(auth)/login/page.tsx
  - /C:/Users/USER/Documents/springboot/analytics/frontend/analytics-app/app/components/AuthGate.tsx

  ### Step A: user enters username and password

  In /C:/Users/USER/Documents/springboot/analytics/frontend/analytics-app/app/(auth)/login/page.tsx, the form collects:

  - username
  - password

  ### Step B: login page calls login()

  The form submits to useAuth().login(username, password).

  ### Step C: auth context sends request to backend

  In /C:/Users/USER/Documents/springboot/analytics/frontend/analytics-app/app/context/AuthContext.tsx, login() calls:

  api.post("/auth/login", { username, password })

  This hits your Spring Boot login endpoint.

  ### Step D: backend returns JWT and user

  The response is expected to contain:

  - jwtToken
  - user

  ### Step E: Next stores session state

  The app stores:

  - authToken
  - analyticsUser

  ### Step F: later requests automatically include auth

  In /C:/Users/USER/Documents/springboot/analytics/frontend/analytics-app/app/lib/api.ts, the Axios interceptor:

  - reads token from localStorage
  - adds Authorization: Bearer <token>

  ### Step G: unsafe requests also include CSRF

  For POST, PUT, PATCH, DELETE:

  - the client checks for XSRF-TOKEN cookie
  - if missing, it fetches /csrf-token
  - then sends X-XSRF-TOKEN header

  ### Step H: protected pages are blocked without auth

  /C:/Users/USER/Documents/springboot/analytics/frontend/analytics-app/app/components/AuthGate.tsx checks:

  - if not authenticated, redirect to /login
  - otherwise render the protected app

  ———

  3. Why CSRF and JWT both exist
  This is a common interview point.

  ### JWT

  JWT proves identity.

  - "Who is the user?"
  - "Is this request coming from a logged-in session?"

  JWT is usually sent in:

  - Authorization: Bearer <token>

  ### CSRF

  CSRF protects unsafe browser requests when cookies are involved.

  - "Did this request really come from my app and not a malicious site?"

  In your case, the frontend may need to use:

  - cookies for CSRF token
  - JWT for user authentication

  So they solve different problems.

  Important interview line:

  - JWT authenticates the user
  - CSRF protects state-changing requests in browser-based flows

  ———

  4. Why withCredentials: true matters
  In /C:/Users/USER/Documents/springboot/analytics/frontend/analytics-app/app/lib/api.ts, Axios is configured with:

  withCredentials: true

  That means:

  - browser is allowed to send cookies
  - backend can set/read CSRF cookies
  - cross-origin requests can include credentialed data if CORS allows it

  Interview angle:

  - Without withCredentials, the browser would not include cookies in cross-origin requests
  - That breaks CSRF cookie-based flows

  ———

  5. Why localStorage is used here
  Your current implementation stores JWT in localStorage.

  ### Pros

  - easy to implement
  - survives refresh
  - simple for interview/demo projects

  ### Cons

  - vulnerable to XSS if your site gets script injection
  - not the most secure production option

  Good interview answer:

  - localStorage is common for demos and simpler SPAs
  - production systems often prefer HttpOnly secure cookies for tokens to reduce XSS risk

  Since you said you want interview readiness, know both:

  - current implementation: localStorage
  - stronger production approach: HttpOnly cookie

  ———

  6. Why route protection is done in Next
  Next.js does not automatically protect routes. You must do it.

  In your app:

  - app/(app)/layout.tsx wraps app pages with AuthGate
  - AuthGate checks auth state
  - if not authenticated, it redirects to /login

  This is the client-side guard.

  Important distinction:

  - client-side guard improves UX
  - backend still must enforce security

  Interview answer:

  - frontend route protection is only a convenience and UX layer
  - real security must always be enforced by the backend

  ———

  7. Why auth state lives in Context
  AuthContext.tsx centralizes:

  - current user
  - current token
  - login/logout actions
  - auth status flags

  This avoids:

  - passing auth props through many components
  - duplicating login logic in multiple pages

  Why this is good:

  - single source of truth
  - easier maintenance
  - cleaner protected UI logic

  Interview answer:

  - Context is used for app-wide auth state because many components need access to login status, user role, and logout behavior

  ———

  8. How request interception works
  The API client is the most important part to understand deeply.

  In /C:/Users/USER/Documents/springboot/analytics/frontend/analytics-app/app/lib/api.ts:

  - request interceptor runs before every request
  - it decides whether to attach token
  - it decides whether to attach CSRF header

  This is better than manually doing it in every page because:

  - less repetition
  - fewer bugs
  - consistent behavior across the app

  Interview answer:

  - interceptors let you apply auth headers globally
  - this is cleaner than manually adding headers to every request

  ———

  9. Why your login request failed before
  Your browser sent a CORS preflight request first.

  Because:

  - frontend origin: http://localhost:3000
  - backend origin: http://localhost:8080

  This is cross-origin.

  For cross-origin POST with JSON and credentials:

  - browser sends OPTIONS preflight
  - backend must allow the origin
  - backend must allow credentials
  - backend must allow headers like Content-Type, Authorization, X-XSRF-TOKEN

  Interview answer:

  - CORS is a browser security mechanism
  - the server must explicitly allow trusted frontend origins

  ———

  10. The login lifecycle you should memorize
  Say this out loud until it becomes natural:

  1. User submits login form
  2. Next.js sends credentials to backend
  3. Backend validates credentials
  4. Backend returns JWT and user data
  5. Next stores token and user locally
  6. Axios adds token automatically to protected requests
  7. CSRF token is fetched for unsafe browser requests
  8. Auth gate protects app routes
  9. Logout clears client session

  That is the end-to-end story.

  ———

  11. What interviewers may ask
  Here are the questions you should be ready for:

  - Why use Context for auth?
  - Why use Axios interceptors?
  - Why do we need withCredentials?
  - What is the difference between JWT and CSRF?
  - Why is localStorage not ideal for production?
  - Why does the browser send an OPTIONS request before login?
  - Why do we protect routes in the frontend if the backend is already secure?
  - How would you refresh the session after page reload?
  - What happens if the token expires?
  - How would you support roles like admin, employer, user?

  ———

  12. Best way to explain your implementation in an interview
  You can say:

  > I built the Next.js client as a session consumer for a Spring Boot backend. The login page posts credentials to the API, receives
  > a JWT and user payload, stores them client-side, and uses an Axios interceptor to attach the JWT to later requests. For unsafe
  > requests, the client also retrieves and sends the CSRF token. Protected pages are wrapped in an auth gate that redirects
  > unauthenticated users to /login.

  That answer sounds strong because it describes:

  - request flow
  - state handling
  - security
  - route protection

  ———

  13. What to learn next on the Next side
  If you want to be interview-ready, study these in order:

  1. CORS and preflight
  2. JWT flow in SPAs
  3. CSRF protection
  4. localStorage vs HttpOnly cookie
  5. Axios interceptors
  6. React Context for app-wide state
  7. Protected routes in Next.js App Router
  8. Session restoration after refresh
  9. Role-based UI rendering
  ———

  14. One important correction to keep in mind
  Your current Next implementation is solid for a project and interview demo, but if you want to say "production-grade", you should
  know:

  - localStorage JWT is not the safest final design
  - HttpOnly cookies are generally safer for token storage
  - CSRF handling becomes even more important when cookies are used

  So for interviews, don’t just say “I stored the JWT in localStorage.”
  Say:

  - “That was my first implementation for simplicity, but for stronger security I would move the token to HttpOnly cookies.”

  That shows real understanding.

  If you want, I can do the next step and teach you the Next.js auth system as a proper interview prep lesson:

  1. from browser request to backend response,
  2. then with diagrams,
  3. then with common interview questions and model answers.


› Update with hands on example from this project  so can I can go  through the code while I read this


• Use these files as your hands-on walkthrough. Read them in this order and trace one login request end to end.

  1. Start with the login form

  - /C:/Users/USER/Documents/springboot/analytics/frontend/analytics-app/app/(auth)/login/page.tsx

  What to look for:

  - username and password state
  - handleSubmit
  - the call to login(username, password)
  - redirect to /dashboard after success

  Key flow:

  const result = await login(username, password);

  if (!result.success) {
    setError(result.error);
    return;
  }

  router.replace("/dashboard");

  This is the UI entry point. Nothing security-related happens here yet. It just collects credentials and calls the auth layer.

  ———

  2. Then inspect the auth state manager

  - /C:/Users/USER/Documents/springboot/analytics/frontend/analytics-app/app/context/AuthContext.tsx

  This is the core file.

  ### On app load

  Look at the useState initializer:

  const [auth, setAuth] = useState(() => {
    const savedUser = localStorage.getItem(AUTH_USER_KEY);
    const savedToken = localStorage.getItem(AUTH_TOKEN_KEY);
    ...
  });

  What this means:

  - the app restores session state from localStorage
  - this is why refresh does not log you out immediately

  ### On login

  Look at:

  const response = await api.post(LOGIN_PATH, { username, password });

  Then:

  setAuth({ user: nextUser, token: jwtToken });

  What this means:

  - the backend returns jwtToken and user
  - the client stores both in context state
  - the useEffect hooks persist them to localStorage

  ### On logout

  Look at:

  await api.post("/auth/logout");
  ...
  setAuth({ user: null, token: null });

  What this means:

  - client asks backend to log out
  - client clears local session state either way

  ### Interview point

  This file is your “single source of truth” for auth in the frontend.

  ———

  3. Check the shared API client

  - /C:/Users/USER/Documents/springboot/analytics/frontend/analytics-app/app/lib/api.ts

  This is where the real request behavior lives.

  ### Base URL

  export const API_BASE_URL =
    process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api";

  This means:

  - local dev uses http://localhost:8080/api
  - production can override with .env.local

  ### JWT attachment

  Look at the request interceptor:

  if (!isPublicEndpoint(config.url)) {
    const token = typeof window !== "undefined" ? localStorage.getItem(AUTH_TOKEN_KEY) : null;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }

  What this means:

  - every protected request gets the bearer token automatically
  - you do not manually add auth headers in every page

  ### CSRF attachment

  Look at:

  if (!isSafeMethod(config.method)) {
    const csrfToken = await ensureCsrfCookie();
    config.headers[CSRF_HEADER_KEY] = csrfToken;
  }

  And:

  await axios.get(`${API_BASE_URL}${CSRF_PATH}`, {
    withCredentials: true,
  });

  What this means:

  - for POST, PUT, PATCH, DELETE, the client ensures a CSRF cookie exists
  - then it sends X-XSRF-TOKEN
  - this mirrors your jobportal-style Spring Boot flow

  ### Why this is important

  This file turns auth from “manual work in every component” into “automatic request behavior.”

  ———

  4. See how protected pages are blocked

  - /C:/Users/USER/Documents/springboot/analytics/frontend/analytics-app/app/components/AuthGate.tsx

  Relevant logic:

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.replace("/login");
    }
  }, [isLoading, isAuthenticated, router]);

  What this means:

  - if there is no session, the user gets redirected
  - protected app pages never render for anonymous users

  And:

  if (isLoading || !isAuthenticated) {
    return <div>Verifying session...</div>;
  }

  What this means:

  - the app avoids flashing protected content before auth is known

  ———

  5. See where the app shell enforces auth

  - /C:/Users/USER/Documents/springboot/analytics/frontend/analytics-app/app/(app)/layout.tsx

  This wraps the real app pages:

  <AuthGate>
    <div className="min-h-screen ...">
      <AppBar />
      <div className="flex ...">
        <Sidebar />
        <main>{children}</main>
      </div>
    </div>
  </AuthGate>

  What this means:

  - dashboard and meter pages are protected at the layout level
  - all app routes under (app) inherit the same guard

  This is a strong Next.js App Router pattern.

  ———

  6. Check the top-level provider wiring

  - /C:/Users/USER/Documents/springboot/analytics/frontend/analytics-app/app/layout.tsx

  Important part:

  <body className="min-h-full">
    <AuthProvider>{children}</AuthProvider>
  </body>

  What this means:

  - every page can access auth state
  - useAuth() works anywhere under the app tree

  ———

  7. Check the logout action in the header

  - /C:/Users/USER/Documents/springboot/analytics/frontend/analytics-app/app/components/LogoutButton.tsx

  Key flow:

  await logout();
  router.replace("/login");

  What this means:

  - one shared logout action
  - UI returns to login after the session is cleared

  ———

  8. Connect it to the browser request flow
  When you submit the login form:

  1. login/page.tsx calls login()
  2. AuthContext.tsx calls api.post("/auth/login")
  3. api.ts sends the request to Spring Boot
  4. Spring Boot returns jwtToken and user
  5. AuthContext.tsx stores both
  6. later requests automatically include Authorization
  7. unsafe requests also include CSRF token
  8. AuthGate.tsx protects the app

  That is the full frontend auth loop.

  ———

  9. The exact code path to trace manually
  If you want to understand it like a senior engineer, follow this path in order:

  1. /C:/Users/USER/Documents/springboot/analytics/frontend/analytics-app/app/(auth)/login/page.tsx
  2. /C:/Users/USER/Documents/springboot/analytics/frontend/analytics-app/app/context/AuthContext.tsx
  3. /C:/Users/USER/Documents/springboot/analytics/frontend/analytics-app/app/lib/api.ts
  4. /C:/Users/USER/Documents/springboot/analytics/frontend/analytics-app/app/components/AuthGate.tsx
  5. /C:/Users/USER/Documents/springboot/analytics/frontend/analytics-app/app/(app)/layout.tsx
  6. /C:/Users/USER/Documents/springboot/analytics/frontend/analytics-app/app/layout.tsx

  ———

  10. Interview-ready summary using your project
  You can explain your implementation like this:

  > In my Next.js frontend, the login page collects credentials and sends them to the Spring Boot /auth/login endpoint. The backend
  > returns a JWT and user object. I store both in client state and localStorage, then use an Axios interceptor to attach the JWT to
  > protected requests. For unsafe requests, I also fetch and send the CSRF token. Protected app routes are wrapped in an auth gate
  > that redirects unauthenticated users to /login.

  If you want, I can do one more step and turn this into a line-by-line code reading guide with:

  1. the exact purpose of each function,
  2. the request/response shape,
  3. and interview questions for each file.
