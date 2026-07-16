import { useEffect, useState } from 'react';
import { Save, Key, Phone, ShieldCheck } from 'lucide-react';
import api, { getApiData } from '../services/api';

export default function WhatsappSettings() {
  const [settings, setSettings] = useState({
    accessToken: '',
    phoneNumberId: '',
  });
  const [loading, setLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const fetchSettings = async () => {
    setLoading(true);
    setError('');

    try {
      const response = await api.get('/api/whatsapp-settings');
      const data = getApiData(response) || {};

      setSettings({
        accessToken: data.accessToken || '',
        phoneNumberId: data.phoneNumberId || '',
      });
    } catch (err) {
      if (err?.response?.status !== 404 && err?.response?.status !== 400) {
        setError(err?.response?.data?.message || 'Failed to load WhatsApp settings.');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSettings();
  }, []);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setSettings((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSave = async () => {
    setIsSaving(true);
    setError('');
    setSuccess('');

    try {
      await api.put('/api/whatsapp-settings', settings);
      setSuccess('WhatsApp settings saved successfully');
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to save settings');
    } finally {
      setIsSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[#1a237e]"></div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">WhatsApp Settings</h1>
        <p className="text-sm text-gray-500 mt-1">
          Configure Meta Cloud API credentials for automated WhatsApp updates
        </p>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="p-6 border-b border-gray-100 bg-gray-50 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-blue-100 text-[#1a237e] rounded-lg">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <h2 className="text-lg font-semibold text-gray-900">API Credentials</h2>
          </div>
          <button
            onClick={handleSave}
            disabled={isSaving}
            className="inline-flex items-center gap-2 px-4 py-2 bg-[#1a237e] hover:bg-[#1a237e]/90 text-white text-sm font-medium rounded-lg transition-colors disabled:opacity-50"
          >
            <Save className="w-4 h-4" />
            {isSaving ? 'Saving...' : 'Save Changes'}
          </button>
        </div>

        <div className="p-6 space-y-6">
          {error && (
            <div className="p-4 bg-red-50 border border-red-200 text-red-600 rounded-lg text-sm">
              {error}
            </div>
          )}

          {success && (
            <div className="p-4 bg-green-50 border border-green-200 text-green-600 rounded-lg text-sm">
              {success}
            </div>
          )}

          <div className="grid grid-cols-1 gap-6">
            <div className="space-y-2">
              <label className="text-sm font-medium text-gray-700 flex items-center gap-2">
                <Phone className="w-4 h-4 text-gray-400" />
                Phone Number ID
              </label>
              <input
                type="text"
                name="phoneNumberId"
                value={settings.phoneNumberId}
                onChange={handleInputChange}
                className="w-full px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-[#1a237e]/20 focus:border-[#1a237e] transition-colors"
                placeholder="e.g. 1079794308560229"
              />
              <p className="text-xs text-gray-500">The unique ID assigned to your business phone number by Meta.</p>
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium text-gray-700 flex items-center gap-2">
                <Key className="w-4 h-4 text-gray-400" />
                Access Token
              </label>
              <textarea
                name="accessToken"
                value={settings.accessToken}
                onChange={handleInputChange}
                rows={4}
                className="w-full px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-[#1a237e]/20 focus:border-[#1a237e] transition-colors resize-y font-mono text-sm"
                placeholder="EAAONesO4G..."
              />
              <p className="text-xs text-gray-500">Your permanent system user token generated from the Meta App Dashboard.</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
