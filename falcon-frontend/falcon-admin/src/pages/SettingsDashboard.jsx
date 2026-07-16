import { useNavigate } from 'react-router-dom';
import { Image as ImageIcon, Settings, MapPin, Building2, Images, Mail, Tag, MessageCircle } from 'lucide-react';

export default function SettingsDashboard() {
  const navigate = useNavigate();

  return (
    <div className="relative flex flex-col h-full pb-6 px-6 pt-6">
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-gray-800">System Settings</h1>
        <p className="text-[0.85rem] text-gray-500 mt-1">Manage your website configurations and preferences.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div onClick={() => navigate('/admin/settings/banners')} className="bg-white p-5 rounded border border-gray-100 shadow-sm hover:border-blue-200 cursor-pointer transition-all group flex items-start gap-3">
          <div className="p-2.5 bg-blue-50 text-blue-600 group-hover:bg-blue-600 group-hover:text-white rounded transition-colors">
            <ImageIcon className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-[0.95rem] font-semibold text-gray-800 group-hover:text-blue-600 transition-colors">Banner Control</h2>
            <p className="text-[0.80rem] text-gray-500 mt-0.5">Manage homepage slider banners and promotional images.</p>
          </div>
        </div>

        <div onClick={() => navigate('/admin/settings/locations')} className="bg-white p-5 rounded border border-gray-100 shadow-sm hover:border-blue-200 cursor-pointer transition-all group flex items-start gap-3">
          <div className="p-2.5 bg-blue-50 text-blue-600 group-hover:bg-blue-600 group-hover:text-white rounded transition-colors">
            <MapPin className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-[0.95rem] font-semibold text-gray-800 group-hover:text-blue-600 transition-colors">Delivery Locations</h2>
            <p className="text-[0.80rem] text-gray-500 mt-0.5">Manage serviceable pin codes and delivery charges.</p>
          </div>
        </div>

        <div onClick={() => navigate('/admin/settings/company-details')} className="bg-white p-5 rounded border border-gray-100 shadow-sm hover:border-blue-200 cursor-pointer transition-all group flex items-start gap-3">
          <div className="p-2.5 bg-blue-50 text-blue-600 group-hover:bg-blue-600 group-hover:text-white rounded transition-colors">
            <Building2 className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-[0.95rem] font-semibold text-gray-800 group-hover:text-blue-600 transition-colors">Company Settings</h2>
            <p className="text-[0.80rem] text-gray-500 mt-0.5">Manage company details, contact information, address, and about section.</p>
          </div>
        </div>

        <div onClick={() => navigate('/admin/settings/company-images')} className="bg-white p-5 rounded border border-gray-100 shadow-sm hover:border-blue-200 cursor-pointer transition-all group flex items-start gap-3">
          <div className="p-2.5 bg-blue-50 text-blue-600 group-hover:bg-blue-600 group-hover:text-white rounded transition-colors">
            <Images className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-[0.95rem] font-semibold text-gray-800 group-hover:text-blue-600 transition-colors">Company Images</h2>
            <p className="text-[0.80rem] text-gray-500 mt-0.5">Manage the company logo, banner, and other brand image assets.</p>
          </div>
        </div>

        <div onClick={() => navigate('/admin/settings/mailer-settings')} className="bg-white p-5 rounded border border-gray-100 shadow-sm hover:border-blue-200 cursor-pointer transition-all group flex items-start gap-3">
          <div className="p-2.5 bg-blue-50 text-blue-600 group-hover:bg-blue-600 group-hover:text-white rounded transition-colors">
            <Mail className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-[0.95rem] font-semibold text-gray-800 group-hover:text-blue-600 transition-colors">Mailer Settings</h2>
            <p className="text-[0.80rem] text-gray-500 mt-0.5">Manage SMTP host, port, username, and password credentials dynamically.</p>
          </div>
        </div>

        <div onClick={() => navigate('/admin/settings/categories')} className="bg-white p-5 rounded border border-gray-100 shadow-sm hover:border-blue-200 cursor-pointer transition-all group flex items-start gap-3">
          <div className="p-2.5 bg-blue-50 text-blue-600 group-hover:bg-blue-600 group-hover:text-white rounded transition-colors">
            <Tag className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-[0.95rem] font-semibold text-gray-800 group-hover:text-blue-600 transition-colors">Categories</h2>
            <p className="text-[0.80rem] text-gray-500 mt-0.5">Manage product categories and subcategories.</p>
          </div>
        </div>

        <div onClick={() => navigate('/admin/settings/whatsapp-settings')} className="bg-white p-5 rounded border border-gray-100 shadow-sm hover:border-blue-200 cursor-pointer transition-all group flex items-start gap-3">
          <div className="p-2.5 bg-blue-50 text-blue-600 group-hover:bg-blue-600 group-hover:text-white rounded transition-colors">
            <MessageCircle className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-[0.95rem] font-semibold text-gray-800 group-hover:text-blue-600 transition-colors">WhatsApp Settings</h2>
            <p className="text-[0.80rem] text-gray-500 mt-0.5">Manage WhatsApp API credentials and templates.</p>
          </div>
        </div>

        <div className="bg-gray-50/50 p-5 rounded border border-gray-100 flex items-start gap-3 opacity-60 cursor-not-allowed">
          <div className="p-2.5 bg-gray-100 text-gray-400 rounded"><Settings className="w-5 h-5" /></div>
          <div>
            <h2 className="text-[0.95rem] font-semibold text-gray-800">General Setup</h2>
            <p className="text-[0.80rem] text-gray-500 mt-0.5">Basic configurations. <span className="text-[0.70rem] text-blue-500 font-medium ml-1">Coming Soon</span></p>
          </div>
        </div>

      </div>
    </div>
  );
}
