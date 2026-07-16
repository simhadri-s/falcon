import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

export default function AdminDashboard() {
  const navigate = useNavigate();
  const [stats, setStats] = useState({
    totalProducts: 0,
    totalInquiries: 0,
    unreadInquiries: 0,
    totalNews: 0,
    totalApplications: 0
  });
  
  // State for recent inquiries table and low stock / expiry notice
  const [recentInquiries, setRecentInquiries] = useState([]);
  const [noticeProducts, setNoticeProducts] = useState([]);
  const [expiringProducts, setExpiringProducts] = useState([]);
  const [loading, setLoading] = useState(true);

  // Parse "DD-MM-YYYY" or "MM-YYYY" into a JS Date.
  // If DD is absent we default to the 1st of the month.
  const parseExpiryDate = (str) => {
    if (!str || typeof str !== 'string') return null;
    const parts = str.trim().split('-');
    if (parts.length === 3) {
      // DD-MM-YYYY
      const [dd, mm, yyyy] = parts;
      const d = new Date(Number(yyyy), Number(mm) - 1, Number(dd));
      return isNaN(d.getTime()) ? null : d;
    } else if (parts.length === 2) {
      // MM-YYYY — use last day of the month so the whole month is still valid.
      // new Date(yyyy, mm, 0) = day 0 of month+1 = last day of mm (1-indexed).
      const [mm, yyyy] = parts;
      const d = new Date(Number(yyyy), Number(mm), 0);
      return isNaN(d.getTime()) ? null : d;
    }
    return null;
  };

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        // 1. Fetch Stats
        const statsResponse = await api.get('/api/stats');
        const statsData = statsResponse.data;
        setStats({
          totalProducts: statsData.TotalProduct || 0,
          totalInquiries: statsData.TotalInquiries || 0,
          unreadInquiries: statsData.UnreadInquiries || 0,
          totalNews: statsData.TotalNews || 0,
          totalApplications: statsData.TotalApplications || 0
        });

        // 2. Fetch Recent Inquiries (Page 1, limit to 5)
        const inquiriesResponse = await api.get('/api/inquiries?page=1&limit=5');
        setRecentInquiries(inquiriesResponse.data.data || []);

        // 3. Fetch Products for Low Stock Notice
        const productsResponse = await api.get('/api/products?page=1&limit=1000');
        const allProducts = productsResponse.data?.data || productsResponse.data || [];
        const now = new Date();
        const in30Days = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000);

        const lowStock = allProducts.filter(p => {
          const qty = p.stockQuantity !== null && p.stockQuantity !== undefined ? Number(p.stockQuantity) : 0;
          return qty < 20;
        });
        setNoticeProducts(lowStock);

        const expiring = allProducts.filter(p => {
          const expDate = parseExpiryDate(p.expiryDate);
          return expDate !== null && expDate <= in30Days;
        });
        setExpiringProducts(expiring);
      } catch (error) {
        console.error("Failed to fetch dashboard data", error);
      } finally {
        setLoading(false);
      }
    };
    
    fetchDashboardData();
  }, []);

  const formatDate = (dateString) => {
    const options = { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' };
    return new Date(dateString).toLocaleDateString('en-US', options);
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'NEW': return <span className="px-2 py-1 text-xs font-bold text-red-800 bg-red-100 rounded-full">NEW</span>;
      case 'READ': return <span className="px-2 py-1 text-xs font-bold text-blue-800 bg-blue-100 rounded-full">READ</span>;
      case 'RESPONDED': return <span className="px-2 py-1 text-xs font-bold text-green-800 bg-green-100 rounded-full">RESPONDED</span>;
      default: return <span className="px-2 py-1 text-xs font-bold text-gray-800 bg-gray-100 rounded-full">{status}</span>;
    }
  };

  return (
    <div className="relative flex flex-col h-full pb-6 px-6 pt-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-xl font-semibold text-gray-800">Dashboard Overview</h1>
        <div className="space-x-2">
          <button 
            onClick={() => navigate('/admin/products')}
            className="px-4 py-2 text-[0.85rem] font-medium text-white bg-blue-600 rounded hover:bg-blue-700 shadow-sm transition-colors"
          >
            Manage Products
          </button>
          <button 
            onClick={() => navigate('/admin/news')}
            className="px-4 py-2 text-[0.85rem] font-medium text-gray-700 bg-white border border-gray-200 rounded hover:bg-gray-50 shadow-sm transition-colors"
          >
            Manage News
          </button>
        </div>
      </div>

      {/* Statistics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <StatCard 
          title="Total Products" 
          value={stats.totalProducts} 
          onClick={() => navigate('/admin/products')} 
        />
        <StatCard 
          title="Total Inquiries" 
          value={stats.totalInquiries} 
          badge={stats.unreadInquiries} 
          onClick={() => navigate('/admin/inquiries')} 
        />
        <StatCard 
          title="News Articles" 
          value={stats.totalNews} 
          onClick={() => navigate('/admin/news')} 
        />
        <StatCard 
          title="Career Apps" 
          value={stats.totalApplications} 
          onClick={() => navigate('/admin/careers')} 
        />
      </div>

      {/* Grid Layout for Recent Inquiries & Notice Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6 flex-1">
        {/* Left Column: Recent Inquiries + Expiring Soon */}
        <div className="flex flex-col gap-6">
          {/* Recent Inquiries */}
          <div className="flex flex-col h-full">
            <div className="flex justify-between items-center mb-3">
              <h2 className="text-[0.95rem] font-semibold text-gray-800">Recent Inquiries</h2>
              <button 
                onClick={() => navigate('/admin/inquiries')}
                className="text-blue-600 hover:underline text-[0.85rem] font-medium"
              >
                View All &rarr;
              </button>
            </div>
            
            <div className="bg-white rounded border border-gray-100 shadow-sm overflow-hidden flex-1">
              <table className="w-full text-left border-collapse">
                <thead className="bg-gray-50 border-b border-gray-100">
                  <tr>
                    <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider">Date</th>
                    <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider">Client</th>
                    <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider">Subject</th>
                    <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider">Status</th>
                    <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider text-right">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {loading ? (
                    <tr><td colSpan="5" className="px-4 py-6 text-center text-[0.85rem] text-gray-500">Loading recent inquiries...</td></tr>
                  ) : recentInquiries.length === 0 ? (
                    <tr><td colSpan="5" className="px-4 py-6 text-center text-[0.85rem] text-gray-500">No recent inquiries found.</td></tr>
                  ) : (
                    recentInquiries.map((inquiry) => {
                      const inquiryId = inquiry.id || inquiry._id;
                      return (
                      <tr key={inquiryId} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                        <td className="px-4 py-3 text-[0.85rem] text-gray-600">{formatDate(inquiry.createdAt)}</td>
                        <td className="px-4 py-3">
                          <div className="text-[0.85rem] font-medium text-gray-800">{inquiry.name}</div>
                        </td>
                        <td className="px-4 py-3 text-[0.85rem] text-gray-700">{inquiry.subject}</td>
                        <td className="px-4 py-3">{getStatusBadge(inquiry.status)}</td>
                        <td className="px-4 py-3 text-right">
                          <button 
                            onClick={() => navigate(`/admin/inquiries/${inquiryId}`)}
                            className="text-blue-600 hover:underline text-[0.85rem] font-medium"
                          >
                            Open
                          </button>
                        </td>
                      </tr>
                    )})
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* Expiring Soon Card */}
          <div className="flex flex-col h-full">
            <div className="flex justify-between items-center mb-3">
              <h2 className="text-[0.95rem] font-semibold text-gray-800">Notice <span className="text-[0.85rem] font-normal text-amber-600">(Expiring within 30 days)</span></h2>
              <button 
                onClick={() => navigate('/admin/products')}
                className="text-blue-600 hover:underline text-[0.85rem] font-medium"
              >
                Manage Products &rarr;
              </button>
            </div>

            <div className="bg-white rounded border border-gray-100 shadow-sm overflow-hidden flex-1">
              <table className="w-full text-left border-collapse">
                <thead className="bg-gray-50 border-b border-gray-100">
                  <tr>
                    <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider">Product</th>
                    <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider">Code</th>
                    <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider text-right">Expires</th>
                  </tr>
                </thead>
                <tbody>
                  {loading ? (
                    <tr><td colSpan="3" className="px-4 py-6 text-center text-[0.85rem] text-gray-500">Loading expiry data...</td></tr>
                  ) : expiringProducts.length === 0 ? (
                    <tr><td colSpan="3" className="px-4 py-6 text-center text-[0.85rem] text-gray-500">All products are within expiry.</td></tr>
                  ) : (
                    expiringProducts.map((product) => {
                      const productId = product.id || product._id;
                      return (
                        <tr 
                          key={productId} 
                          onClick={() => navigate('/admin/products', { state: { editProduct: product } })}
                          className="border-b border-gray-50 hover:bg-gray-50 cursor-pointer transition-all"
                        >
                          <td className="px-4 py-3 flex items-center gap-3">
                            {product.imageUrls && product.imageUrls.length > 0 ? (
                              <img src={product.imageUrls[0]} alt={product.name} className="w-8 h-8 object-cover rounded border border-gray-200 shrink-0" />
                            ) : (
                              <div className="w-8 h-8 bg-gray-100 rounded border border-gray-200 flex items-center justify-center text-[10px] text-gray-400 shrink-0">No Img</div>
                            )}
                            <div className="text-[0.85rem] font-medium text-gray-800 line-clamp-1">{product.name}</div>
                          </td>
                          <td className="px-4 py-3 text-[0.85rem] text-gray-600 font-mono">
                            {product.productCode || '-'}
                          </td>
                          <td className="px-4 py-3 text-right">
                            <span className={`px-2 py-1 text-[0.75rem] font-medium rounded border ${
                              parseExpiryDate(product.expiryDate) < new Date() 
                                ? 'text-white bg-red-600 border-red-700' 
                                : 'text-amber-800 bg-amber-100 border-amber-300'
                            }`}>
                              {parseExpiryDate(product.expiryDate) < new Date() ? 'EXPIRED' : product.expiryDate}
                            </span>
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        {/* Right Column: Low Stock Notice */}
        <div className="flex flex-col gap-6">

          {/* Low Stock Card */}
          <div className="flex flex-col h-full">
            <div className="flex justify-between items-center mb-3">
              <h2 className="text-[0.95rem] font-semibold text-gray-800">Notice <span className="text-[0.85rem] font-normal text-red-600">(Stock &lt; 20)</span></h2>
              <button 
                onClick={() => navigate('/admin/products')}
                className="text-blue-600 hover:underline text-[0.85rem] font-medium"
              >
                Manage Inventory &rarr;
              </button>
            </div>
            
            <div className="bg-white rounded border border-gray-100 shadow-sm overflow-hidden flex-1">
              <table className="w-full text-left border-collapse">
                <thead className="bg-gray-50 border-b border-gray-100">
                  <tr>
                    <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider">Product</th>
                    <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider">Code</th>
                    <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider text-right">Stock</th>
                  </tr>
                </thead>
                <tbody>
                  {loading ? (
                    <tr><td colSpan="3" className="px-4 py-6 text-center text-[0.85rem] text-gray-500">Loading notice items...</td></tr>
                  ) : noticeProducts.length === 0 ? (
                    <tr><td colSpan="3" className="px-4 py-6 text-center text-[0.85rem] text-gray-500">No products with stock less than 20.</td></tr>
                  ) : (
                    noticeProducts.map((product) => {
                      const productId = product.id || product._id;
                      const qty = product.stockQuantity !== null && product.stockQuantity !== undefined ? Number(product.stockQuantity) : 0;
                      return (
                        <tr 
                          key={productId} 
                          onClick={() => navigate('/admin/products', { state: { editProduct: product } })}
                          className="border-b border-gray-50 hover:bg-gray-50 cursor-pointer transition-all"
                        >
                          <td className="px-4 py-3 flex items-center gap-3">
                            {product.imageUrls && product.imageUrls.length > 0 ? (
                              <img src={product.imageUrls[0]} alt={product.name} className="w-8 h-8 object-cover rounded border border-gray-200 shrink-0" />
                            ) : (
                              <div className="w-8 h-8 bg-gray-100 rounded border border-gray-200 flex items-center justify-center text-[10px] text-gray-400 shrink-0">No Img</div>
                            )}
                            <div className="text-[0.85rem] font-medium text-gray-800 line-clamp-1">{product.name}</div>
                          </td>
                          <td className="px-4 py-3 text-[0.85rem] text-gray-600 font-mono">
                            {product.productCode || '-'}
                          </td>
                          <td className="px-4 py-3 text-right">
                            <span className="px-2 py-1 text-[0.75rem] font-medium text-red-800 bg-red-100 rounded border border-red-200">
                              {qty} Left
                            </span>
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

// Reusable StatCard Component
function StatCard({ title, value, badge, onClick }) {
  return (
    <div 
      onClick={onClick}
      className={`p-5 bg-white rounded border border-gray-100 shadow-sm relative transition-all duration-200 ${onClick ? 'cursor-pointer hover:border-blue-300 hover:shadow-md' : ''}`}
    >
      {badge > 0 && (
        <span className="absolute top-4 right-4 bg-red-500 text-white text-[0.70rem] font-bold px-2 py-0.5 rounded shadow-sm">
          {badge} New
        </span>
      )}
      <h3 className="text-gray-500 text-[0.75rem] font-semibold uppercase tracking-wider mb-1">{title}</h3>
      <p className="text-2xl font-bold text-gray-800">{value}</p>
    </div>
  );
}