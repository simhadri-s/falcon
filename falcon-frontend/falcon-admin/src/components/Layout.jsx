import { useEffect, useState } from 'react';
import { NavLink, Outlet, useNavigate, useLocation } from 'react-router-dom';
import api from '../services/api';
import { useCompany } from '../context/CompanyContext';
import { eraseTokenCookie } from '../utils/cookies';

import { 
  UsersRound, 
  Factory, 
  Settings, 
  ShoppingCart, 
  Users,
  Building2,
  Images,
  MapPin,
  Image as ImageIcon,
  LayoutDashboard, 
  PackageSearch, 
  Newspaper, 
  MessageSquareText, 
  BriefcaseBusiness, 
  LogOut, 
  CircleUserRound,
  Bell,
  Mail,
  Tag,
  Star,
  Undo2,
  Banknote,
  BarChart3,
  Search,
  X,
  MessageCircle
} from 'lucide-react';

export default function Layout() {
  const navigate = useNavigate();
  const { companyName, logoUrl } = useCompany();
  const location = useLocation();
  const [newOrdersCount, setNewOrdersCount] = useState(0);
  const [menuSearch, setMenuSearch] = useState('');

  const menuItems = [
    { to: "/admin/dashboard", label: "Dashboard", icon: LayoutDashboard },
    { to: "/admin/products", label: "Products", icon: PackageSearch },
    { to: "/admin/reviews", label: "Reviews", icon: Star },
    { 
      to: "/admin/orders", 
      label: "Orders", 
      icon: ShoppingCart, 
      badge: newOrdersCount > 0 ? newOrdersCount : null 
    },
    { to: "/admin/returns", label: "Returns", icon: Undo2 },
    { to: "/admin/refunds", label: "Refunds", icon: Banknote },
    { to: "/admin/news", label: "News", icon: Newspaper },
    { to: "/admin/inquiries", label: "Inquiries", icon: MessageSquareText },

    { to: "/admin/careers", label: "Careers", icon: BriefcaseBusiness },
    { to: "/admin/users", label: "Users & Roles", icon: UsersRound },
    { to: "/admin/customers", label: "Customers", icon: Users },
    { to: "/admin/reports", label: "Reports", icon: BarChart3 },
    { to: "/admin/industries", label: "Industries", icon: Factory },
    { to: "/admin/notifications", label: "Notifications", icon: Bell },
    { to: "/admin/coupons", label: "Coupons", icon: Tag },
  ];

  const systemMenuItems = [
    { to: "/admin/settings", label: "Settings Dashboard", icon: Settings, end: true, isSub: false },
    { to: "/admin/settings/company-details", label: "Company Details", icon: Building2, isSub: true },
    { to: "/admin/settings/company-images", label: "Company Images", icon: Images, isSub: true },
    { to: "/admin/settings/banners", label: "Banner Control", icon: ImageIcon, isSub: true },
    { to: "/admin/settings/locations", label: "Delivery Locations", icon: MapPin, isSub: true },
    { to: "/admin/settings/categories", label: "Categories", icon: Tag, isSub: true },
    { to: "/admin/settings/mailer-settings", label: "Mailer Settings", icon: Mail, isSub: true },
    { to: "/admin/settings/whatsapp-settings", label: "WhatsApp Settings", icon: MessageCircle, isSub: true },
  ];

  const filteredMenuItems = menuItems.filter(item => 
    item.label.toLowerCase().includes(menuSearch.toLowerCase())
  );

  const filteredSystemMenuItems = systemMenuItems.filter(item => 
    item.label.toLowerCase().includes(menuSearch.toLowerCase())
  );

  const fetchNewOrdersCount = async () => {
    try {
      const res = await api.post('/api/orders/status-count', { statusType: 'CREATED' });
      setNewOrdersCount(res.data.count || 0);
    } catch (err) {
      console.error("Failed to fetch new orders count:", err);
    }
  };

  useEffect(() => {
    const initialLoad = setTimeout(fetchNewOrdersCount, 0);

    // Poll every 30 seconds for new orders
    const interval = setInterval(fetchNewOrdersCount, 30000);
    return () => {
      clearTimeout(initialLoad);
      clearInterval(interval);
    };
  }, [location.pathname]);

  const handleLogout = () => {
    eraseTokenCookie();
    if (localStorage.getItem('token')) {
      localStorage.removeItem('token');
    }
    navigate('/admin/login');
  };

  const getNavLinkClass = ({ isActive }) => {
    return `flex items-center p-3 rounded transition-colors duration-200 font-medium mb-2 ${
      isActive 
        ? 'bg-[#E8A020] text-white shadow-md'
        : 'text-gray-200 hover:bg-[#152B47] hover:text-white'
    }`;
  };

  const getSubNavLinkClass = ({ isActive }) => {
    return `flex items-center pl-9 pr-3 py-2 rounded text-sm transition-colors duration-200 font-medium mb-1.5 ${
      isActive 
        ? 'bg-[#E8A020]/20 text-[#E8A020] border-l-2 border-[#E8A020]'
        : 'text-gray-300 hover:bg-[#152B47]/50 hover:text-white'
    }`;
  };

  return (
    <div className="flex h-screen bg-gray-50">
      {/* Sidebar Navigation */}
      <aside className="w-64 bg-[#1E3A5F] text-white flex flex-col shadow-xl z-10">
        <div className="p-5 flex flex-col items-center justify-center border-b border-gray-700 gap-2.5">
          {logoUrl ? (
            <img 
              src={logoUrl} 
              alt={companyName} 
              className="h-12 w-auto object-contain max-w-[90%] bg-white/10 rounded px-2 py-1" 
            />
          ) : null}
          <div className="text-lg font-black text-center tracking-wider text-white uppercase line-clamp-1">
            {companyName}
          </div>
        </div>
        {/* Menu Search Bar */}
        <div className="px-4 pt-4 pb-2 border-b border-gray-700/50">
          <div className="relative">
            <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
              <Search className="w-4 h-4 text-gray-400" />
            </div>
            <input 
              type="text" 
              placeholder="Search menus..." 
              value={menuSearch} 
              onChange={(e) => setMenuSearch(e.target.value)} 
              className="w-full pl-9 pr-8 py-2 text-xs bg-[#152B47] text-white placeholder-gray-400 border border-gray-700 rounded focus:outline-none focus:ring-1 focus:ring-[#E8A020] focus:border-[#E8A020] transition-colors"
            />
            {menuSearch && (
              <button 
                onClick={() => setMenuSearch('')} 
                className="absolute inset-y-0 right-0 flex items-center pr-2.5 text-gray-400 hover:text-white"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>
        
        <nav className="flex-1 p-4 overflow-y-auto mt-2">
          {filteredMenuItems.length === 0 && filteredSystemMenuItems.length === 0 ? (
            <div className="text-center text-gray-400 text-sm py-4">
              No matching menus found
            </div>
          ) : (
            <>
              {filteredMenuItems.map((item) => {
                const Icon = item.icon;
                return (
                  <NavLink key={item.to} to={item.to} className={getNavLinkClass}>
                    {item.badge !== undefined ? (
                      <div className="flex items-center justify-between w-full">
                        <div className="flex items-center">
                          <Icon className="w-5 h-5 mr-3" />
                          {item.label}
                        </div>
                        {item.badge && (
                          <span className="bg-red-500 text-white text-[10px] font-extrabold px-2 py-0.5 rounded-full shadow-sm animate-pulse">
                            {item.badge}
                          </span>
                        )}
                      </div>
                    ) : (
                      <>
                        <Icon className="w-5 h-5 mr-3" />
                        {item.label}
                      </>
                    )}
                  </NavLink>
                );
              })}

              {filteredSystemMenuItems.length > 0 && (
                <>
                  <div className="mt-6 mb-2 px-4 text-xs font-bold text-gray-400 uppercase tracking-wider">
                    System
                  </div>

                  {filteredSystemMenuItems.map((item) => {
                    const Icon = item.icon;
                    return (
                      <NavLink 
                        key={item.to}
                        to={item.to} 
                        end={item.end}
                        className={item.isSub ? getSubNavLinkClass : getNavLinkClass}
                      >
                        <Icon className={`${item.isSub ? 'w-4 h-4 mr-2.5' : 'w-5 h-5 mr-3'}`} />
                        {item.label}
                      </NavLink>
                    );
                  })}
                </>
              )}
            </>
          )}
        </nav>
        
        <div className="p-4 border-t border-gray-700">
          <button 
            onClick={handleLogout} 
            className="flex items-center justify-center w-full p-3 bg-red-600 hover:bg-red-700 rounded text-white font-bold transition-colors shadow-sm"
          >
            <LogOut className="w-5 h-5 mr-2" />
            Logout
          </button>
        </div>
      </aside>

      <main className="flex-1 flex flex-col overflow-hidden">
        <header className="p-4 bg-white shadow-sm flex justify-end items-center border-b z-0">
          <div className="flex items-center gap-2 px-3 py-1.5 bg-gray-100 rounded-full border border-gray-200">
            <CircleUserRound className="w-6 h-6 text-[#1E3A5F]" />
            <span className="font-semibold text-gray-700 text-sm pr-1">Admin User</span>
          </div>
        </header>
        
        <div className="admin-content p-6 overflow-auto flex-1 bg-gray-50">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
