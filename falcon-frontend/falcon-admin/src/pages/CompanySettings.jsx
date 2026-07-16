import { useEffect, useState } from 'react';
import { Save } from 'lucide-react';
import api, { getApiData } from '../services/api';
import { useCompany } from '../context/CompanyContext';

const INITIAL_COMPANY_INFO = {
  companyName: '',
  email: '',
  phone: '',
  address: '',
  workingHours: '',
  termsAndConditions: '',
  privacyPolicy: '',
  orderIdPrefix: '',
};

const CompanySettings = () => {
  const { fetchCompanyData } = useCompany();
  const [companyInfo, setCompanyInfo] = useState(INITIAL_COMPANY_INFO);
  const [loading, setLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [hasExistingSettings, setHasExistingSettings] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const fetchCompanySettings = async () => {
    setLoading(true);
    setError('');

    try {
      const response = await api.get('/api/company-settings');
      const data = getApiData(response) || {};

      setCompanyInfo({
        companyName: data.companyName || '',
        email: data.email || '',
        phone: data.phone || '',
        address: data.address || '',
        workingHours: data.workingHours || '',
        termsAndConditions: data.termsAndConditions || '',
        privacyPolicy: data.privacyPolicy || '',
        orderIdPrefix: data.orderIdPrefix || '',
      });
      setHasExistingSettings(true);
    } catch (err) {
      const isMissingSettings =
        err?.response?.status === 400 &&
        err?.response?.data?.message === 'Company settings not found';

      setCompanyInfo(INITIAL_COMPANY_INFO);
      setHasExistingSettings(false);

      if (!isMissingSettings) {
        setError(err?.response?.data?.message || 'Failed to load company settings.');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCompanySettings();
  }, []);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setCompanyInfo((current) => ({ ...current, [name]: value }));
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setIsSaving(true);
    setError('');
    setSuccess('');

    try {
      const payload = {
        companyName: companyInfo.companyName.trim(),
        email: companyInfo.email.trim(),
        phone: companyInfo.phone.trim(),
        address: companyInfo.address.trim(),
        workingHours: companyInfo.workingHours.trim(),
        termsAndConditions: companyInfo.termsAndConditions.trim(),
        privacyPolicy: companyInfo.privacyPolicy.trim(),
        orderIdPrefix: companyInfo.orderIdPrefix.trim(),
      };

      const response = hasExistingSettings
        ? await api.put('/api/company-settings', payload)
        : await api.post('/api/company-settings', payload);

      const savedSettings = getApiData(response) || payload;

      setCompanyInfo({
        companyName: savedSettings.companyName || '',
        email: savedSettings.email || '',
        phone: savedSettings.phone || '',
        address: savedSettings.address || '',
        workingHours: savedSettings.workingHours || '',
        termsAndConditions: savedSettings.termsAndConditions || '',
        privacyPolicy: savedSettings.privacyPolicy || '',
        orderIdPrefix: savedSettings.orderIdPrefix || '',
      });
      setHasExistingSettings(true);
      setSuccess('Company settings saved successfully.');
      await fetchCompanyData();
    } catch (err) {
      console.error('Failed to save company settings:', err);
      setError(err?.response?.data?.message || 'Failed to save company settings.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="relative flex flex-col h-full pb-6 px-6 pt-6">
      <form onSubmit={handleSave} className="flex flex-col h-full">
        <header className="mb-6 flex items-center justify-between">
          <div>
            <h1 className="text-xl font-semibold text-gray-800">Company Settings</h1>
            <p className="text-[0.85rem] text-gray-500 mt-1">
              Manage core company details, address, contact info, and working hours.
            </p>
          </div>

          <button
            type="submit"
            disabled={loading || isSaving}
            className="flex items-center gap-1.5 px-4 py-2 text-white bg-blue-600 rounded text-[0.85rem] hover:bg-blue-700 font-medium shadow-sm transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
          >
            <Save className="w-4 h-4" />
            {isSaving ? 'Saving...' : 'Save Settings'}
          </button>
        </header>

        {error && <p className="mb-4 text-red-500 bg-red-50 p-3 rounded text-[0.85rem] border border-red-100 max-w-3xl">{error}</p>}
        {success && <p className="mb-4 text-green-700 bg-green-50 p-3 rounded text-[0.85rem] border border-green-100 max-w-3xl">{success}</p>}

        <div className="bg-white p-6 rounded border border-gray-100 shadow-sm max-w-3xl">
          {loading ? (
            <div className="text-[0.85rem] text-gray-500 p-4">Loading company settings...</div>
          ) : (
            <div className="grid grid-cols-1 gap-5">
              <div>
                <label className="block mb-1.5 font-medium text-[0.85rem] text-gray-700">Company Name</label>
                <input
                  type="text"
                  name="companyName"
                  required
                  value={companyInfo.companyName}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none transition-colors"
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                <div>
                  <label className="block mb-1.5 font-medium text-[0.85rem] text-gray-700">Contact Email</label>
                  <input
                    type="email"
                    name="email"
                    value={companyInfo.email}
                    onChange={handleInputChange}
                    className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none transition-colors"
                  />
                </div>

                <div>
                  <label className="block mb-1.5 font-medium text-[0.85rem] text-gray-700">Phone Number</label>
                  <input
                    type="text"
                    name="phone"
                    value={companyInfo.phone}
                    onChange={handleInputChange}
                    className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none transition-colors"
                  />
                </div>
              </div>

              <div>
                <label className="block mb-1.5 font-medium text-[0.85rem] text-gray-700">Headquarters Address</label>
                <input
                  type="text"
                  name="address"
                  value={companyInfo.address}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none transition-colors"
                />
              </div>

              <div>
                <label className="block mb-1.5 font-medium text-[0.85rem] text-gray-700">Working Hours</label>
                <input
                  type="text"
                  name="workingHours"
                  value={companyInfo.workingHours}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none transition-colors"
                  placeholder="e.g. Monday to Saturday, 9 AM - 6 PM"
                />
              </div>
              
              <div>
                <label className="block mb-1.5 font-medium text-[0.85rem] text-gray-700">Order ID Prefix</label>
                <input
                  type="text"
                  name="orderIdPrefix"
                  value={companyInfo.orderIdPrefix}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none transition-colors uppercase"
                  placeholder="e.g. FAL"
                  maxLength={4}
                />
                <p className="text-[0.75rem] text-gray-400 mt-1">Maximum 4 characters recommended. Total Order ID will be 10 characters.</p>
              </div>

              <div>
                <label className="block mb-1.5 font-medium text-[0.85rem] text-gray-700">Terms & Conditions</label>
                <textarea
                  name="termsAndConditions"
                  rows="6"
                  value={companyInfo.termsAndConditions}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none transition-colors"
                  placeholder="Enter terms and conditions content..."
                />
              </div>

              <div>
                <label className="block mb-1.5 font-medium text-[0.85rem] text-gray-700">Privacy Policy</label>
                <textarea
                  name="privacyPolicy"
                  rows="6"
                  value={companyInfo.privacyPolicy}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none transition-colors"
                  placeholder="Enter privacy policy content..."
                />
              </div>
            </div>
          )}
        </div>
      </form>
    </div>
  );
};

export default CompanySettings;
