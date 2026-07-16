import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../services/api';
import { useCompany } from '../context/CompanyContext';

export default function AdminRegister() {
  const navigate = useNavigate();
  const { companyName } = useCompany();

  // Form states
  const [name, setName] = useState(''); 
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  
  // UI states
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleRegister = async (e) => {
    e.preventDefault();
    setError('');
    setSuccessMsg('');

    // 1. Check if passwords match
    if (password !== confirmPassword) {
      setError('Passwords do not match!');
      return;
    }

    // 2. Minimum password length check (Optional but recommended)
    if (password.length < 6) {
      setError('Password must be at least 6 characters long.');
      return;
    }

    setIsLoading(true);
    try {
      await api.post('/api/auth/register', { 
        name: name, // 🌟 ಬ್ಯಾಕೆಂಡ್‌ಗೆ ಹೆಸರನ್ನು ಕಳುಹಿಸುತ್ತಿದ್ದೇವೆ
        email: email, 
        password: password 
      });
      
      setSuccessMsg('Registration successful! Redirecting to login...');
      
      // Clear the form
      setName(''); // 🌟 ಹೆಸರನ್ನು ಕ್ಲಿಯರ್ ಮಾಡುವುದು
      setEmail('');
      setPassword('');
      setConfirmPassword('');

      // Redirect to login page after a short delay so they see the success message
      setTimeout(() => {
        navigate('/admin/login');
      }, 2000);

    } catch (err) {
      console.error("Registration error:", err);
      // Show error message from backend if available, otherwise generic error
      setError(err.response?.data?.message || 'Failed to register. Email might already be in use.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-50 px-4">
      <div className="w-full max-w-md p-8 bg-white rounded-lg shadow-sm border border-gray-100">
        
        <div className="text-center mb-8">
          <h1 className="text-[1.1rem] font-medium tracking-normal text-gray-800">
            {companyName ? companyName.toUpperCase() : "FALCON LASER"}
          </h1>
          <p className="text-gray-500 mt-1">Admin Registration</p>
        </div>

        {error && <div className="p-3 mb-4 text-sm text-red-700 bg-red-100 rounded border border-red-200">{error}</div>}
        {successMsg && <div className="p-3 mb-4 text-sm text-green-700 bg-green-100 rounded border border-green-200">{successMsg}</div>}

        <form onSubmit={handleRegister}>
          
          {/* 🌟 ಹೊಸದಾಗಿ ಸೇರಿಸಿದ Full Name ಬಾಕ್ಸ್ (New Name Field) */}
          <div className="mb-4">
            <label className="block mb-1 text-sm font-medium text-gray-700">Full Name <span className="text-red-500">*</span></label>
            <input 
              type="text" 
              className="w-full p-2 border rounded focus:ring-[#1E3A5F] focus:border-[#1E3A5F]" 
              value={name} 
              onChange={(e) => setName(e.target.value)} 
              placeholder="e.g. John Doe"
              required 
            />
          </div>

          <div className="mb-4">
            <label className="block mb-1 text-sm font-medium text-gray-700">Email Address <span className="text-red-500">*</span></label>
            <input 
              type="email" 
              className="w-full p-2 border rounded focus:ring-[#1E3A5F] focus:border-[#1E3A5F]" 
              value={email} 
              onChange={(e) => setEmail(e.target.value)} 
              placeholder="admin@falconlaser.com"
              required 
            />
          </div>
          
          <div className="mb-4">
            <label className="block mb-1 text-sm font-medium text-gray-700">Password <span className="text-red-500">*</span></label>
            <input 
              type="password" 
              className="w-full p-2 border rounded focus:ring-[#1E3A5F] focus:border-[#1E3A5F]" 
              value={password} 
              onChange={(e) => setPassword(e.target.value)} 
              placeholder="Create a strong password"
              required 
            />
          </div>

          <div className="mb-6">
            <label className="block mb-1 text-sm font-medium text-gray-700">Confirm Password <span className="text-red-500">*</span></label>
            <input 
              type="password" 
              className="w-full p-2 border rounded focus:ring-[#1E3A5F] focus:border-[#1E3A5F]" 
              value={confirmPassword} 
              onChange={(e) => setConfirmPassword(e.target.value)} 
              placeholder="Repeat your password"
              required 
            />
          </div>

          <button 
            type="submit" 
            disabled={isLoading}
            className="w-full p-2 text-white bg-[#2563eb] rounded hover:bg-blue-600 font-medium disabled:opacity-50 transition shadow-sm mb-4"
          >
            {isLoading ? 'Creating Account...' : 'Register Admin'}
          </button>
          
          <div className="text-center mt-4 border-t pt-4">
            <p className="text-sm text-gray-600">
              Already have an account?{' '}
              <Link to="/admin/login" className="font-medium text-blue-600 hover:underline">
                Log in here
              </Link>
            </p>
          </div>
        </form>

      </div>
    </div>
  );
}
