import React, { createContext, useContext, useState, useEffect } from 'react';
import api, { getApiData } from '../services/api';

const CompanyContext = createContext();

export const useCompany = () => useContext(CompanyContext);

export const CompanyProvider = ({ children }) => {
  const [companySettings, setCompanySettings] = useState(null);
  const [companyImage, setCompanyImage] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchCompanyData = async () => {
    try {
      // Fetch settings
      try {
        const settingsRes = await api.get('/api/company-settings');
        const settingsData = getApiData(settingsRes);
        if (settingsData) {
          setCompanySettings(settingsData);
        }
      } catch (err) {
        console.error('Error fetching company settings in context:', err);
      }

      // Fetch images
      try {
        const imagesRes = await api.get('/api/company-images');
        const imagesData = getApiData(imagesRes);
        if (Array.isArray(imagesData) && imagesData.length > 0) {
          setCompanyImage(imagesData[0]); // assuming the first one is active/main
        }
      } catch (err) {
        console.error('Error fetching company images in context:', err);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCompanyData();
  }, []);

  const companyName = companySettings?.companyName || companyImage?.name || 'Falcon Laser';
  const logoUrl = companyImage?.logoUrl || '';
  const faviconUrl = companyImage?.faviconUrl || '';
  const iconUrl = companyImage?.iconUrl || '';

  // Update favicon dynamically
  useEffect(() => {
    if (faviconUrl) {
      let link = document.querySelector("link[rel~='icon']");
      if (!link) {
        link = document.createElement('link');
        link.rel = 'icon';
        document.getElementsByTagName('head')[0].appendChild(link);
      }
      link.href = faviconUrl;
    }
  }, [faviconUrl]);

  // Update document title dynamically
  useEffect(() => {
    if (companyName) {
      document.title = `${companyName} - Admin`;
    }
  }, [companyName]);

  return (
    <CompanyContext.Provider value={{
      companySettings,
      companyImage,
      companyName,
      logoUrl,
      faviconUrl,
      iconUrl,
      loading,
      fetchCompanyData
    }}>
      {children}
    </CompanyContext.Provider>
  );
};
