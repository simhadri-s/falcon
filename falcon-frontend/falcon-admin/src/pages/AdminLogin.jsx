import { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import api from '../services/api';
import { setTokenCookie } from '../utils/cookies';
import { useCompany } from '../context/CompanyContext';

export default function AdminLogin() {
  const navigate = useNavigate();
  const location = useLocation();
  const { companyName } = useCompany();

  // View state: 'login' | 'request-otp' | 'reset-password'
  const [view, setView] = useState('login');

  // Form states
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');

  // UI states
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // --- Check for OAuth2 Token in URL ---
  // When Google Login finishes, your backend success handler should redirect back here like: /admin/login?token=xyz...
  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const token = params.get('token');

    if (token) {
      setTokenCookie(token);
      navigate('/admin/dashboard');
    }
  }, [location, navigate]);

  // --- Handlers ---

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);
    try {
      const response = await api.post('/api/auth/login', { email, password });
      setTokenCookie(response.data.token);
      navigate('/admin/dashboard');
    } catch (err) {
      setError('Invalid email or password');
    } finally {
      setIsLoading(false);
    }
  };

  const handleGoogleLogin = () => {
    // Redirect directly to Spring Boot OAuth2 endpoint
    window.location.href = 'http://localhost:8080/oauth2/authorization/google';
  };

  // --- UPDATED: Using new /forgot-password endpoint ---
  const handleSendOtp = async (e) => {
    e.preventDefault();
    setError('');
    setSuccessMsg('');
    setIsLoading(true);
    try {
      const response = await api.post('/api/auth/forgot-password', { email });
      setView('reset-password');
      setSuccessMsg(response.data.message || 'OTP sent to your email successfully!');
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data?.error || 'Failed to send OTP. Please check your email address.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    setError('');
    setSuccessMsg('');
    setIsLoading(true);
    try {
      const response = await api.post('/api/auth/reset-password', {
        email: email,
        password: newPassword,
        otp: otp
      });
      setView('login');
      setSuccessMsg(response.data.message || 'Password reset successful! You can now log in.');
      setPassword('');
      setOtp('');
      setNewPassword('');
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data?.error || 'Failed to reset password. Please check the OTP.');
    } finally {
      setIsLoading(false);
    }
  };

  // --- Render Helpers ---

  const renderLoginView = () => (
    <form onSubmit={handleLogin}>
      <div className="mb-4">
        <label className="block mb-1 text-sm font-medium text-gray-700">Email</label>
        <input
          type="email"
          className="w-full p-2 border rounded focus:ring-[#1E3A5F] focus:border-[#1E3A5F]"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
      </div>
      <div className="mb-2">
        <label className="block mb-1 text-sm font-medium text-gray-700">Password</label>
        <input
          type="password"
          className="w-full p-2 border rounded focus:ring-[#1E3A5F] focus:border-[#1E3A5F]"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
      </div>

      <div className="flex justify-end mb-6">
        <button
          type="button"
          onClick={() => { setView('request-otp'); setError(''); setSuccessMsg(''); }}
          className="text-sm text-blue-600 hover:underline"
        >
          Forgot Password?
        </button>
      </div>

      <button
        type="submit"
        disabled={isLoading}
        className="w-full p-2 text-white bg-[#2563eb] rounded hover:bg-blue-600 font-medium disabled:opacity-50 transition"
      >
        {isLoading ? 'Logging in...' : 'Login'}
      </button>

      <div className="relative flex items-center justify-center w-full mt-6 mb-6 border-t border-gray-300">
        <span className="absolute px-2 text-sm text-gray-500 bg-white">OR</span>
      </div>

      <button
        type="button"
        onClick={handleGoogleLogin}
        className="flex items-center justify-center w-full p-2 text-gray-700 transition bg-white border border-gray-300 rounded shadow-sm hover:bg-gray-50"
      >
        <img src="https://www.svgrepo.com/show/475656/google-color.svg" alt="Google" className="w-5 h-5 mr-2" />
        <span className="font-medium">Sign in with Google</span>
      </button>

      {/* 🌟 ಹೊಸದಾಗಿ ಸೇರಿಸಿದ ರಿಜಿಸ್ಟರ್ ಲಿಂಕ್ (New Register Link) */}
      <div className="mt-6 text-center text-sm text-gray-600">
        Don't have an account?{' '}
        <Link to="/admin/register" className="font-medium text-blue-600 hover:underline transition-colors">
          Create one now
        </Link>
      </div>
    </form>
  );

  const renderRequestOtpView = () => (
    <form onSubmit={handleSendOtp}>
      <p className="mb-4 text-sm text-gray-600">
        Enter your email address and we will send you a One-Time Password (OTP) to reset your password.
      </p>
      <div className="mb-6">
        <label className="block mb-1 text-sm font-medium text-gray-700">Email Address</label>
        <input
          type="email"
          className="w-full p-2 border rounded focus:ring-[#1E3A5F]"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
      </div>

      <button
        type="submit"
        disabled={isLoading}
        className="w-full p-2 text-white bg-[#E8A020] rounded hover:bg-yellow-600 font-medium disabled:opacity-50 transition mb-4"
      >
        {isLoading ? 'Sending...' : 'Send OTP'}
      </button>

      <button
        type="button"
        onClick={() => { setView('login'); setError(''); }}
        className="w-full p-2 text-gray-600 bg-gray-100 rounded hover:bg-gray-200 font-medium transition"
      >
        Back to Login
      </button>
    </form>
  );

  const renderResetPasswordView = () => (
    <form onSubmit={handleResetPassword}>
      <p className="mb-4 text-sm text-gray-600">
        Please check your email <strong>{email}</strong> for the OTP and enter your new password.
      </p>
      <div className="mb-4">
        <label className="block mb-1 text-sm font-medium text-gray-700">Enter OTP</label>
        <input
          type="text"
          className="w-full p-2 border rounded focus:ring-[#1E3A5F] tracking-widest text-center"
          value={otp}
          onChange={(e) => setOtp(e.target.value)}
          placeholder="e.g. 596491"
          required
        />
      </div>
      <div className="mb-6">
        <label className="block mb-1 text-sm font-medium text-gray-700">New Password</label>
        <input
          type="password"
          className="w-full p-2 border rounded focus:ring-[#1E3A5F]"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          required
        />
      </div>

      <button
        type="submit"
        disabled={isLoading}
        className="w-full p-2 text-white bg-green-600 rounded hover:bg-green-700 font-medium disabled:opacity-50 transition mb-4"
      >
        {isLoading ? 'Resetting...' : 'Reset Password'}
      </button>

      <button
        type="button"
        onClick={() => { setView('login'); setError(''); setSuccessMsg(''); }}
        className="w-full p-2 text-gray-600 bg-gray-100 rounded hover:bg-gray-200 font-medium transition"
      >
        Cancel
      </button>
    </form>
  );

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-50 px-4">
      <div className="w-full max-w-md p-8 bg-white rounded-lg shadow-sm border border-gray-100">

        <div className="text-center mb-8">
          <h1 className="text-[1.1rem] font-medium tracking-normal text-gray-800">
            {companyName ? companyName.toUpperCase() : "FALCON LASER"}
          </h1>
          <p className="text-gray-500 mt-1">Admin Portal</p>
        </div>

        {error && <div className="p-3 mb-4 text-sm text-red-700 bg-red-100 rounded">{error}</div>}
        {successMsg && <div className="p-3 mb-4 text-sm text-green-700 bg-green-100 rounded">{successMsg}</div>}

        {view === 'login' && renderLoginView()}
        {view === 'request-otp' && renderRequestOtpView()}
        {view === 'reset-password' && renderResetPasswordView()}

      </div>
    </div>
  );
}
