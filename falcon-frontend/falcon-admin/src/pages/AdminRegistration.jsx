import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../services/api';

export default function AdminRegister() {
  const navigate = useNavigate();

  // Form State
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: ''
  });

  // UI States
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setError('');
    setSuccessMsg('');

    // 🌟 ಪಾಸ್‌ವರ್ಡ್ ಮ್ಯಾಚ್ ಆಗುತ್ತದೆಯೇ ಎಂದು ಪರೀಕ್ಷಿಸುವುದು (Password Match Validation)
    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match! Please check again.');
      return;
    }

    // ಪಾಸ್‌ವರ್ಡ್ ಕನಿಷ್ಠ 6 ಅಕ್ಷರ ಇರಬೇಕು (Optional length check)
    if (formData.password.length < 6) {
      setError('Password must be at least 6 characters long.');
      return;
    }

    setIsLoading(true);
    try {
      // ಬ್ಯಾಕೆಂಡ್‌ಗೆ ಕಳುಹಿಸಬೇಕಾದ ಡೇಟಾ (confirmPassword ಕಳುಹಿಸುವ ಅಗತ್ಯವಿಲ್ಲ)
      const payload = {
        name: formData.name,
        email: formData.email,
        password: formData.password
      };

      await api.post('/api/auth/register', payload);
      
      setSuccessMsg('Registration successful! Redirecting to login...');
      
      // ರಿಜಿಸ್ಟರ್ ಆದ 2 ಸೆಕೆಂಡ್‌ಗಳ ನಂತರ ಲಾಗಿನ್ ಪೇಜ್‌ಗೆ ಹೋಗುವುದು
      setTimeout(() => {
        navigate('/admin/login');
      }, 2000);

    } catch (err) {
      setError(err.response?.data?.message || err.response?.data?.error || 'Registration failed. Email might already exist.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-100 px-4">
      <div className="w-full max-w-md p-8 bg-white rounded-lg shadow-xl border-t-4 border-[#1E3A5F]">
        
        {/* Header section matching the Login page */}
        <div className="text-center mb-8">
          <h1 className="text-2xl font-black tracking-wider text-[#1E3A5F]">
            FALCON <span className="text-[#E8A020]">LASER</span>
          </h1>
          <p className="text-gray-500 mt-1 font-medium">Create an Account</p>
        </div>

        {error && <div className="p-3 mb-4 text-sm font-medium text-red-700 bg-red-50 border border-red-200 rounded">{error}</div>}
        {successMsg && <div className="p-3 mb-4 text-sm font-medium text-green-700 bg-green-50 border border-green-200 rounded">{successMsg}</div>}

        <form onSubmit={handleRegister}>
          <div className="mb-4">
            <label className="block mb-1 text-sm font-bold text-gray-700">Full Name</label>
            <input 
              type="text" 
              name="name"
              className="w-full p-2.5 border border-gray-300 rounded focus:ring-2 focus:ring-[#1E3A5F] focus:border-[#1E3A5F] outline-none transition-shadow" 
              value={formData.name} 
              onChange={handleInputChange} 
              placeholder="e.g. John Doe"
              required 
            />
          </div>

          <div className="mb-4">
            <label className="block mb-1 text-sm font-bold text-gray-700">Email Address</label>
            <input 
              type="email" 
              name="email"
              className="w-full p-2.5 border border-gray-300 rounded focus:ring-2 focus:ring-[#1E3A5F] focus:border-[#1E3A5F] outline-none transition-shadow" 
              value={formData.email} 
              onChange={handleInputChange} 
              placeholder="admin@example.com"
              required 
            />
          </div>

          <div className="mb-4">
            <label className="block mb-1 text-sm font-bold text-gray-700">Password</label>
            <input 
              type="password" 
              name="password"
              className="w-full p-2.5 border border-gray-300 rounded focus:ring-2 focus:ring-[#1E3A5F] focus:border-[#1E3A5F] outline-none transition-shadow" 
              value={formData.password} 
              onChange={handleInputChange} 
              placeholder="••••••••"
              required 
            />
          </div>

          <div className="mb-6">
            <label className="block mb-1 text-sm font-bold text-gray-700">Confirm Password</label>
            <input 
              type="password" 
              name="confirmPassword"
              className={`w-full p-2.5 border rounded outline-none transition-shadow ${
                formData.confirmPassword && formData.password !== formData.confirmPassword 
                  ? 'border-red-500 focus:ring-2 focus:ring-red-500 bg-red-50' 
                  : 'border-gray-300 focus:ring-2 focus:ring-[#1E3A5F] focus:border-[#1E3A5F]'
              }`} 
              value={formData.confirmPassword} 
              onChange={handleInputChange} 
              placeholder="••••••••"
              required 
            />
            {/* Real-time mismatch warning */}
            {formData.confirmPassword && formData.password !== formData.confirmPassword && (
              <p className="text-xs text-red-500 mt-1 font-bold">Passwords do not match</p>
            )}
          </div>

          <button 
            type="submit" 
            disabled={isLoading}
            className="w-full p-3 text-white bg-[#1E3A5F] rounded-lg hover:bg-blue-900 font-bold disabled:opacity-70 disabled:cursor-not-allowed transition-colors shadow-md"
          >
            {isLoading ? 'Creating Account...' : 'Register'}
          </button>

          {/* Back to Login Link */}
          <div className="mt-6 text-center text-sm text-gray-600">
            Already have an account?{' '}
            <Link to="/admin/login" className="font-bold text-[#E8A020] hover:text-yellow-600 hover:underline transition-colors">
              Login here
            </Link>
          </div>
        </form>

      </div>
    </div>
  );
}