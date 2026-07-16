import { useEffect, useState } from 'react';
import { Save, Mail, Server, Key, ShieldCheck } from 'lucide-react';
import api, { getApiData } from '../services/api';

const INITIAL_MAILER_INFO = {
  mailHost: 'smtp.gmail.com',
  mailPort: 587,
  mailUsername: '',
  mailPassword: '',
};

const MailerSettings = () => {
  const [mailerInfo, setMailerInfo] = useState(INITIAL_MAILER_INFO);
  const [loading, setLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [hasExistingSettings, setHasExistingSettings] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const fetchMailerSettings = async () => {
    setLoading(true);
    setError('');

    try {
      const response = await api.get('/api/mailer-settings');
      const data = getApiData(response) || {};

      setMailerInfo({
        mailHost: data.mailHost || 'smtp.gmail.com',
        mailPort: data.mailPort || 587,
        mailUsername: data.mailUsername || '',
        mailPassword: data.mailPassword || '',
      });
      setHasExistingSettings(true);
    } catch (err) {
      setMailerInfo(INITIAL_MAILER_INFO);
      setHasExistingSettings(false);
      if (err?.response?.status !== 404 && err?.response?.status !== 400) {
        setError(err?.response?.data?.message || 'Failed to load mailer settings.');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMailerSettings();
  }, []);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setMailerInfo((current) => ({
      ...current,
      [name]: name === 'mailPort' ? parseInt(value) || '' : value,
    }));
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setIsSaving(true);
    setError('');
    setSuccess('');

    try {
      const payload = {
        mailHost: mailerInfo.mailHost.trim(),
        mailPort: parseInt(mailerInfo.mailPort) || 587,
        mailUsername: mailerInfo.mailUsername.trim(),
        mailPassword: mailerInfo.mailPassword,
      };

      const response = hasExistingSettings
        ? await api.put('/api/mailer-settings', payload)
        : await api.post('/api/mailer-settings', payload);

      const savedSettings = getApiData(response) || payload;

      setMailerInfo({
        mailHost: savedSettings.mailHost || 'smtp.gmail.com',
        mailPort: savedSettings.mailPort || 587,
        mailUsername: savedSettings.mailUsername || '',
        mailPassword: savedSettings.mailPassword || '',
      });
      setHasExistingSettings(true);
      setSuccess('Mailer configuration saved successfully.');
    } catch (err) {
      console.error('Failed to save mailer settings:', err);
      setError(err?.response?.data?.message || 'Failed to save mailer settings.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="relative flex flex-col h-full pb-6 px-6 pt-6">
      <form onSubmit={handleSave} className="flex flex-col h-full">
        <header className="mb-6 flex items-center justify-between">
          <div>
            <h1 className="text-xl font-semibold text-gray-800">Mailer Settings</h1>
            <p className="text-[0.85rem] text-gray-500 mt-1">
              Configure SMTP credentials dynamically to send system emails and alerts.
            </p>
          </div>

          <button
            type="submit"
            disabled={loading || isSaving}
            className="flex items-center gap-1.5 px-4 py-2 text-[0.85rem] text-white bg-blue-600 rounded hover:bg-blue-700 font-medium shadow-sm transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
          >
            <Save className="w-4 h-4" />
            {isSaving ? 'Saving...' : 'Save Configuration'}
          </button>
        </header>

        {error && <p className="mb-4 text-red-500 bg-red-50 p-3 rounded text-[0.85rem] border border-red-100 max-w-2xl">{error}</p>}
        {success && <p className="mb-4 text-green-700 bg-green-50 p-3 rounded text-[0.85rem] border border-green-100 max-w-2xl">{success}</p>}

        <div className="bg-white p-6 rounded border border-gray-100 shadow-sm max-w-2xl">
          {loading ? (
            <div className="text-[0.85rem] text-gray-500">Loading mailer configurations...</div>
          ) : (
            <div className="grid grid-cols-1 gap-5">
              
              <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
                <div className="md:col-span-2">
                  <label className="block mb-1.5 text-[0.85rem] font-medium text-gray-700 flex items-center gap-1.5">
                    <Server className="w-4 h-4 text-blue-500" />
                    SMTP Host
                  </label>
                  <input
                    type="text"
                    name="mailHost"
                    required
                    value={mailerInfo.mailHost}
                    onChange={handleInputChange}
                    className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none transition-colors"
                    placeholder="smtp.gmail.com"
                  />
                </div>

                <div>
                  <label className="block mb-1.5 text-[0.85rem] font-medium text-gray-700">SMTP Port</label>
                  <input
                    type="number"
                    name="mailPort"
                    required
                    value={mailerInfo.mailPort}
                    onChange={handleInputChange}
                    className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none transition-colors"
                    placeholder="587"
                  />
                </div>
              </div>

              <div>
                <label className="block mb-1.5 text-[0.85rem] font-medium text-gray-700 flex items-center gap-1.5">
                  <Mail className="w-4 h-4 text-orange-500" />
                  SMTP Username (Sender Email)
                </label>
                <input
                  type="email"
                  name="mailUsername"
                  required
                  value={mailerInfo.mailUsername}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none transition-colors"
                  placeholder="e.g. youvideontube@gmail.com"
                />
              </div>

              <div>
                <label className="block mb-1.5 text-[0.85rem] font-medium text-gray-700 flex items-center gap-1.5">
                  <Key className="w-4 h-4 text-amber-500" />
                  SMTP / App Password
                </label>
                <input
                  type="password"
                  name="mailPassword"
                  value={mailerInfo.mailPassword}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none transition-colors"
                  placeholder="Enter SMTP App Password"
                />
              </div>

              <div className="mt-2 p-4 bg-blue-50 border border-blue-100 rounded flex items-start gap-3">
                <ShieldCheck className="w-5 h-5 text-blue-600 shrink-0 mt-0.5" />
                <div className="text-[0.80rem] text-blue-800 leading-relaxed">
                  <p className="font-semibold mb-1">Security Recommendation:</p>
                  For Gmail, use a 16-character <strong>App Password</strong> generated in your Google Account Security settings rather than your primary Google account password. Ensure StartTLS protocol (Port 587) is open.
                </div>
              </div>

            </div>
          )}
        </div>
      </form>
    </div>
  );
};

export default MailerSettings;
