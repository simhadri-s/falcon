/**
 * Decodes a JWT token and checks if it is expired.
 * @param {string} token - The JWT token to check.
 * @returns {boolean} - True if the token is expired or invalid, false otherwise.
 */
export const isTokenExpired = (token) => {
  if (!token) return true;
  try {
    const base64Url = token.split('.')[1];
    if (!base64Url) return true;
    
    // Decode base64URL
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      window.atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    
    const { exp } = JSON.parse(jsonPayload);
    if (!exp) return false; // Default to valid if no expiration claim is present
    
    const currentTime = Math.floor(Date.now() / 1000);
    return exp < currentTime;
  } catch (error) {
    console.error("Error decoding token:", error);
    return true; // Treat as expired/invalid if decoding fails
  }
};

/**
 * Sets the 'token' cookie with an expiration date synchronized with the JWT's exp claim.
 * If no exp claim is found, defaults to an expiration of 4 days.
 * @param {string} token - The JWT token.
 */
export const setTokenCookie = (token) => {
  if (!token) return;
  try {
    const base64Url = token.split('.')[1];
    if (base64Url) {
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        window.atob(base64)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      
      const { exp } = JSON.parse(jsonPayload);
      if (exp) {
        const expiresDate = new Date(exp * 1000);
        document.cookie = `token=${token}; expires=${expiresDate.toUTCString()}; path=/; SameSite=Lax; Secure`;
        return;
      }
    }
  } catch (error) {
    console.error("Error setting token cookie from JWT exp claim:", error);
  }
  
  // Fallback: 4 days expiration
  const date = new Date();
  date.setTime(date.getTime() + (4 * 24 * 60 * 60 * 1000));
  document.cookie = `token=${token}; expires=${date.toUTCString()}; path=/; SameSite=Lax; Secure`;
};

/**
 * Gets the 'token' cookie value.
 * @returns {string|null} - The token value if found, null otherwise.
 */
export const getTokenCookie = () => {
  const nameEQ = "token=";
  const ca = document.cookie.split(';');
  for (let i = 0; i < ca.length; i++) {
    let c = ca[i];
    while (c.charAt(0) === ' ') c = c.substring(1, c.length);
    if (c.indexOf(nameEQ) === 0) return c.substring(nameEQ.length, c.length);
  }
  return null;
};

/**
 * Erases/Deletes the 'token' cookie.
 */
export const eraseTokenCookie = () => {
  document.cookie = 'token=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT; SameSite=Lax; Secure';
};
