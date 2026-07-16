import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Search, FilterX, ArrowUpDown, ArrowUp, ArrowDown, Eye, XCircle, Package, MapPin, ClipboardList, CheckCircle, Clock, Truck, ExternalLink, Download, X } from 'lucide-react';
import api from '../services/api';
import { downloadOrderReceipt } from '../services/orderReceipts';

export default function AdminOrders() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // --- NEW: Status Counts State ---
  const [statusCounts, setStatusCounts] = useState({
    CREATED: 0,
    CONFIRMED: 0,
    PROCESSING: 0,
    SHIPPED: 0
  });

  // Search, Filter & Sort States
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [sortConfig, setSortConfig] = useState({ key: 'createdAt', direction: 'desc' });

  // Pagination
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(10);

  // View Modal State
  const [viewingOrder, setViewingOrder] = useState(null);
  const [downloadingReceiptId, setDownloadingReceiptId] = useState(null);

  // Export CSV States & Logic
  const [exportDaysRange, setExportDaysRange] = useState('10');

  const exportOrdersToCSV = async () => {
    try {
      // 1. Fetch all orders for export to ensure we get everything
      const response = await api.get('/api/orders', { params: { limit: 100000 } });
      let allOrders = response.data.data || [];
      
      // 2. Sort chronologically descending
      allOrders = allOrders.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
      
      // 3. Filter by date range
      let ordersToExport = allOrders;
      if (exportDaysRange !== 'all') {
        const days = parseInt(exportDaysRange);
        const cutoffDate = new Date();
        cutoffDate.setDate(cutoffDate.getDate() - days);
        ordersToExport = allOrders.filter(order => new Date(order.createdAt) >= cutoffDate);
      }
      
      if (ordersToExport.length === 0) {
        alert("No orders available to export for the selected date range.");
        return;
      }

      // 4. Define headers for CSV
    const headers = [
      'Order ID',
      'Customer Email',
      'Order Date',
      'Order Status',
      'Items Count',
      'Ordered Products Detail',
      'Recipient Name',
      'Recipient Phone',
      'Street Address',
      'City',
      'Pincode',
      'Country'
    ];

    // Helper to escape CSV fields
    const escapeCSV = (val) => {
      if (val === null || val === undefined) return '';
      let str = String(val);
      // Escape double quotes by doubling them, wrap string in quotes if it contains commas or quotes
      str = str.replace(/"/g, '""');
      if (str.includes(',') || str.includes('"') || str.includes('\n') || str.includes('\r')) {
        return `"${str}"`;
      }
      return str;
    };

    // 4. Construct CSV rows
    const csvRows = [headers.join(',')];

    ordersToExport.forEach(order => {
      const id = order.id || order._id || '';
      const email = order.userId || '';
      const date = new Date(order.createdAt).toLocaleString();
      const status = order.status || '';
      const count = order.items?.length || 0;
      
      // Combine product snapshots details
      const productsDetail = order.items
        ?.map(item => `${item.productSnapshot?.name || 'Product'} (Qty: ${item.quantity})`)
        .join('; ') || '';

      const address = order.addressSnapshot || {};
      const fullName = address.fullName || '';
      const phone = address.phoneNumber || '';
      const street = address.street || '';
      const city = address.city || '';
      const pincode = address.pincode || '';
      const country = address.country || '';

      const row = [
        escapeCSV(id),
        escapeCSV(email),
        escapeCSV(date),
        escapeCSV(status),
        escapeCSV(count),
        escapeCSV(productsDetail),
        escapeCSV(fullName),
        escapeCSV(phone),
        escapeCSV(street),
        escapeCSV(city),
        escapeCSV(pincode),
        escapeCSV(country)
      ];

      csvRows.push(row.join(','));
    });

    // 5. Download Blob
    const csvContent = "\uFEFF" + csvRows.join("\n"); // Add BOM for Excel support
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute("download", `falcon_orders_last_${exportDaysRange}_days_${new Date().toISOString().slice(0,10)}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    } catch (err) {
      alert("Failed to export orders. Please try again.");
    }
  };

  // Fetch all orders
  const fetchOrders = async () => {
    setLoading(true);
    try {
      const params = {
        page: currentPage,
        limit: itemsPerPage,
      };
      if (searchTerm) params.keyword = searchTerm;
      if (statusFilter) params.status = statusFilter;
      if (sortConfig.key) {
        params.sortBy = sortConfig.key;
        params.sortDirection = sortConfig.direction;
      }
      
      const response = await api.get('/api/orders', { params });
      setOrders(response.data.data || []);
      setTotalPages(response.data.pages || 1);
    } catch (err) {
      setError("Failed to fetch orders.");
    } finally {
      setLoading(false);
    }
  };

  const fetchStatusCounts = async () => {
    try {
      const statuses = ['CREATED', 'CONFIRMED', 'PROCESSING', 'SHIPPED'];
      
      const countPromises = statuses.map(status => 
        api.post('/api/orders/status-count', { statusType: status })
           .then(res => ({ status, count: res.data.count }))
           .catch(err => {
              console.error(`Error fetching ${status}:`, err);
              return { status, count: 0 };
           })
      );

      const results = await Promise.all(countPromises);
      
      const newCounts = {};
      results.forEach(result => {
        newCounts[result.status] = result.count;
      });
      
      setStatusCounts(newCounts);
    } catch (err) {
      console.error("Failed to fetch status counts", err);
    }
  };

  useEffect(() => { 
    fetchStatusCounts(); // Load counts on mount
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => { fetchOrders(); }, 500);
    return () => clearTimeout(timer);
  }, [currentPage, itemsPerPage, searchTerm, statusFilter, sortConfig]);

  const updateOrderStatus = async (orderId, newStatus) => {
    try {
      const response = await api.put(`/api/orders/${orderId}/status`, { orderStatus: newStatus });
      fetchOrders(); 
      fetchStatusCounts(); // Refresh counts when status changes
      
      if (viewingOrder && (viewingOrder.id || viewingOrder._id) === orderId) {
        setViewingOrder(response.data); 
      }
    } catch (err) {
      alert("Failed to update order status.");
    }
  };

  const cancelOrder = async (orderId) => {
    if (window.confirm("Are you sure you want to cancel this order?")) {
      try {
        await api.put(`/api/orders/${orderId}/cancel`);
        fetchOrders();
        fetchStatusCounts(); // Refresh counts
        if (viewingOrder && (viewingOrder.id || viewingOrder._id) === orderId) {
          setViewingOrder({ ...viewingOrder, status: 'CANCELLED' });
        }
      } catch (err) {
        alert(err.response?.data?.message || "Failed to cancel order. It might already be shipped.");
      }
    }
  };

  const handleDownloadReceipt = async (orderId) => {
    if (!orderId || downloadingReceiptId === orderId) return;

    setDownloadingReceiptId(orderId);
    try {
      await downloadOrderReceipt(orderId);
    } catch (err) {
      alert(err.message || 'Failed to download receipt.');
    } finally {
      setDownloadingReceiptId(null);
    }
  };

  const handleSort = (key) => {
    if (sortConfig.key === key) {
      if (sortConfig.direction === 'desc') {
        setSortConfig({ key, direction: 'asc' });
      } else if (sortConfig.direction === 'asc') {
        setSortConfig({ key: null, direction: null });
      }
    } else {
      setSortConfig({ key, direction: 'desc' });
    }
  };

  const getSortIcon = (columnName) => {
    if (sortConfig.key !== columnName || !sortConfig.key) return <ArrowUpDown className="w-3 h-3 ml-2 text-gray-300" />;
    return sortConfig.direction === 'asc' ? <ArrowUp className="w-3 h-3 ml-2 text-gray-500" /> : <ArrowDown className="w-3 h-3 ml-2 text-gray-500" />;
  };

  const getStatusColor = (status) => {
    switch (status?.toUpperCase()) {
      case 'CREATED': return 'border-cyan-400 text-cyan-500';
      case 'CONFIRMED': return 'border-blue-400 text-blue-500'; 
      case 'PROCESSING': return 'border-amber-400 text-amber-500';
      case 'SHIPPED': return 'border-purple-400 text-purple-500';
      case 'OUT_FOR_DELIVERY': return 'border-orange-400 text-orange-500'; 
      case 'DELIVERED': return 'border-emerald-500 text-emerald-600';
      case 'CANCELLED': return 'border-red-400 text-red-500';
      default: return 'border-gray-300 text-gray-500';
    }
  };

  return (
    <div className="bg-white p-6 rounded-lg shadow-sm min-h-screen font-sans">
      <h1 className="text-[1.1rem] font-medium text-gray-800 mb-5">Orders</h1>

      {/* Top Controls */}
      <div className="flex flex-col sm:flex-row justify-between items-center gap-4 mb-6">
        {/* Left side: Keep search/filters */}
        <div className="flex items-center gap-3 w-full sm:w-auto">
          <div className="relative">
            <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 transform -translate-y-1/2" />
            <input 
              type="text" 
              placeholder="Search orders..." 
              value={searchTerm} 
              onChange={(e) => { setSearchTerm(e.target.value); setCurrentPage(1); }} 
              className="pl-9 pr-4 py-1.5 border border-gray-200 rounded text-[0.85rem] outline-none focus:border-blue-400 w-64"
            />
          </div>
          <select 
            value={statusFilter} 
            onChange={(e) => { setStatusFilter(e.target.value); setCurrentPage(1); }} 
            className="border border-gray-200 rounded px-3 py-1.5 text-[0.85rem] text-gray-500 outline-none focus:border-blue-400 bg-white"
          >
            <option value="">All Statuses</option>
            <option value="CREATED">CREATED</option>
            <option value="CONFIRMED">CONFIRMED</option>
            <option value="PROCESSING">PROCESSING</option>
            <option value="SHIPPED">SHIPPED</option>
            <option value="OUT_FOR_DELIVERY">OUT FOR DELIVERY</option>
            <option value="DELIVERED">DELIVERED</option>
            <option value="CANCELLED">CANCELLED</option>
          </select>
        </div>

        {/* Right side: Sort By and Action Button */}
        <div className="flex flex-wrap items-center gap-4 justify-end">
          <div className="flex items-center gap-2">
            <span className="text-[0.85rem] text-gray-600">Sort By :</span>
            <select 
              value={sortConfig.key || ''} 
              onChange={(e) => handleSort(e.target.value)}
              className="border border-gray-200 rounded px-3 py-1.5 text-[0.85rem] text-gray-500 outline-none focus:border-blue-400 bg-white min-w-[100px]"
            >
              <option value="">Sort</option>
              <option value="createdAt">Date</option>
              <option value="userId">Customer</option>
              <option value="status">Status</option>
            </select>
          </div>

          <div className="flex items-center gap-2 border-l pl-4 border-gray-200">
            <select 
              value={exportDaysRange} 
              onChange={(e) => setExportDaysRange(e.target.value)} 
              className="border border-gray-200 rounded px-3 py-1.5 text-[0.85rem] text-gray-500 outline-none focus:border-blue-400 bg-white"
            >
              <option value="10">Last 10 Days</option>
              <option value="50">Last 50 Days</option>
              <option value="100">Last 100 Days</option>
              <option value="all">All Time</option>
            </select>
            <button 
              onClick={exportOrdersToCSV}
              className="bg-[#2563eb] hover:bg-blue-600 text-white px-4 py-1.5 rounded text-[0.85rem] font-medium transition-colors shadow-sm flex items-center gap-1.5"
            >
              <Download className="w-4 h-4" /> Export CSV
            </button>
          </div>
        </div>
      </div>

      {error && <p className="mb-4 text-red-500 bg-red-50 p-3 rounded text-sm">{error}</p>}

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse min-w-[900px]">
          <thead>
            <tr>
              <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap">
                <div className="flex items-center cursor-pointer" onClick={() => handleSort('id')}>
                  Order # {getSortIcon('id')}
                </div>
              </th>
              <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap">
                <div className="flex items-center cursor-pointer" onClick={() => handleSort('userId')}>
                  Customer {getSortIcon('userId')}
                </div>
              </th>
              <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap">
                <div className="flex items-center cursor-pointer" onClick={() => handleSort('createdAt')}>
                  Date {getSortIcon('createdAt')}
                </div>
              </th>
              <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap">
                Items
              </th>
              <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap">
                <div className="flex items-center cursor-pointer" onClick={() => handleSort('status')}>
                  Status {getSortIcon('status')}
                </div>
              </th>
              <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap">
                <div className="flex items-center">
                  Actions {getSortIcon('actions')}
                </div>
              </th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="6" className="p-8 text-center text-gray-500 text-sm">Loading orders...</td></tr>
            ) : orders.length === 0 ? (
              <tr><td colSpan="6" className="p-8 text-center text-gray-500 text-sm">No orders found.</td></tr>
            ) : (
              orders.map((order) => {
                const orderIdStr = order.id || order._id;
                
                return (
                <tr key={orderIdStr} className="hover:bg-gray-50/50 transition-colors">
                  <td className="py-4 px-4 text-[0.85rem] text-gray-600 border-b border-gray-100">
                    WD-{orderIdStr.slice(-4).toUpperCase()}
                  </td>
                  <td className="py-4 px-4 text-[0.85rem] text-gray-600 border-b border-gray-100">{order.userId}</td>
                  <td className="py-4 px-4 text-[0.85rem] text-gray-600 border-b border-gray-100">{new Date(order.createdAt).toLocaleDateString()}</td>
                  <td className="py-4 px-4 text-[0.85rem] text-gray-600 border-b border-gray-100">{order.items?.length || 0}</td>
                  <td className="py-4 px-4 border-b border-gray-100">
                    <select 
                      value={order.status} 
                      onChange={(e) => updateOrderStatus(orderIdStr, e.target.value)}
                      disabled={order.status === 'CANCELLED' || order.status === 'DELIVERED'}
                      className={`bg-transparent text-[0.7rem] font-medium pl-2.5 pr-6 py-1 rounded border outline-none max-w-[135px] truncate ${getStatusColor(order.status)} ${(order.status === 'CANCELLED' || order.status === 'DELIVERED') ? 'opacity-70 cursor-not-allowed' : 'cursor-pointer'}`}
                    >
                      <option value="CREATED">CREATED</option>
                      <option value="CONFIRMED">CONFIRMED</option>
                      <option value="PROCESSING">PROCESSING</option>
                      <option value="SHIPPED">SHIPPED</option>
                      <option value="OUT_FOR_DELIVERY">OUT FOR DELIVERY</option>
                      <option value="DELIVERED">DELIVERED</option>
                      {order.status === 'CANCELLED' && <option value="CANCELLED">CANCELLED</option>}
                    </select>
                  </td>
                  <td className="py-4 px-4 border-b border-gray-100">
                    <div className="flex items-center gap-4">
                      <button onClick={() => setViewingOrder(order)} className="flex items-center gap-1 text-[0.85rem] font-medium text-gray-800 hover:text-blue-600 transition-colors">
                        <Eye className="w-3.5 h-3.5 text-blue-600" /> View
                      </button>
                      <button 
                        onClick={() => handleDownloadReceipt(orderIdStr)} 
                        disabled={downloadingReceiptId === orderIdStr}
                        className={`flex items-center gap-1 text-[0.85rem] font-medium transition-colors ${
                          downloadingReceiptId === orderIdStr 
                            ? 'text-gray-400 cursor-not-allowed opacity-50' 
                            : 'text-gray-800 hover:text-emerald-600'
                        }`}
                      >
                        <Download className={`w-3.5 h-3.5 ${downloadingReceiptId === orderIdStr ? 'text-gray-400 animate-bounce' : 'text-emerald-500'}`} /> 
                        {downloadingReceiptId === orderIdStr ? 'Loading...' : 'Receipt'}
                      </button>
                      {order.status !== 'CANCELLED' && order.status !== 'SHIPPED' && order.status !== 'OUT_FOR_DELIVERY' && order.status !== 'DELIVERED' && (
                        <button onClick={() => cancelOrder(orderIdStr)} className="flex items-center gap-1 text-[0.85rem] font-medium text-gray-800 hover:text-red-600 transition-colors">
                          <span className="text-red-500 font-bold leading-none mb-[2px]">&times;</span> Remove
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              )})
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {!loading && orders.length > 0 && (
        <div className="flex items-center justify-between mt-6 text-[0.85rem] text-gray-500">
          <div>
            Showing {(currentPage - 1) * itemsPerPage + 1} to {Math.min(currentPage * itemsPerPage, (currentPage - 1) * itemsPerPage + orders.length)} of {totalPages * itemsPerPage} entries
          </div>
          <div className="flex items-center gap-1">
            <button onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))} disabled={currentPage === 1} className="px-3 py-1 border border-gray-200 rounded hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed text-gray-600">Previous</button>
            
            {Array.from({ length: Math.min(totalPages, 4) }).map((_, idx) => (
              <button 
                key={idx} 
                onClick={() => setCurrentPage(idx + 1)}
                className={`px-3 py-1 border rounded ${currentPage === idx + 1 ? 'bg-gray-100 border-gray-300 text-gray-800' : 'border-transparent hover:bg-gray-50 text-gray-600'}`}
              >
                {idx + 1}
              </button>
            ))}
            
            <button onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))} disabled={currentPage === totalPages} className="px-3 py-1 border border-gray-200 rounded hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed text-gray-600">Next</button>
          </div>
        </div>
      )}

      {/* VIEW ORDER MODAL */}
      {viewingOrder && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="w-full max-w-3xl bg-white rounded shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
            <div className="p-6 border-b border-gray-100 flex justify-between items-center">
              <div>
                <h2 className="text-lg font-semibold text-gray-800">Order Details</h2>
                <p className="text-[0.80rem] text-gray-500 font-mono mt-1">ID: {viewingOrder.id || viewingOrder._id}</p>
              </div>
              <button onClick={() => setViewingOrder(null)} className="text-gray-400 hover:text-red-500 transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>
            
            <div className="p-6 overflow-y-auto flex-1">
              
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                <div className="p-4 bg-white rounded border border-gray-100 shadow-sm">
                  <p className="text-[0.70rem] font-medium text-gray-500 uppercase tracking-wider mb-1">Customer Email</p>
                  <p className="text-[0.85rem] font-medium text-gray-900 break-all">{viewingOrder.userId}</p>
                </div>
                <div className="p-4 bg-white rounded border border-gray-100 shadow-sm">
                  <p className="text-[0.70rem] font-medium text-gray-500 uppercase tracking-wider mb-1">Order Date</p>
                  <p className="text-[0.85rem] font-medium text-gray-900">{new Date(viewingOrder.createdAt).toLocaleString()}</p>
                </div>
                <div className="p-4 bg-white rounded border border-gray-100 shadow-sm">
                  <p className="text-[0.70rem] font-medium text-gray-500 uppercase tracking-wider mb-1">Current Status</p>
                  <span className={`inline-block px-2.5 py-0.5 text-[0.70rem] font-medium rounded border mt-1 ${getStatusColor(viewingOrder.status)}`}>
                    {viewingOrder.status}
                  </span>
                </div>
              </div>

              <h3 className="text-[0.9rem] font-medium text-gray-800 mb-3 flex items-center border-b border-gray-100 pb-2">
                <MapPin className="w-4 h-4 mr-2 text-gray-400" /> Shipping Details
              </h3>
              
              {viewingOrder.addressSnapshot ? (
                <div className="mb-6 bg-white p-4 rounded border border-gray-100 shadow-sm flex flex-col md:flex-row gap-4 justify-between items-start">
                  <div>
                    <p className="text-[0.9rem] font-semibold text-gray-800 mb-1">{viewingOrder.addressSnapshot.fullName}</p>
                    <p className="text-[0.85rem] text-gray-600 leading-relaxed">{viewingOrder.addressSnapshot.street}</p>
                    <p className="text-[0.85rem] text-gray-600">{viewingOrder.addressSnapshot.city} - <span className="font-medium text-gray-800">{viewingOrder.addressSnapshot.pincode}</span></p>
                    <p className="text-[0.80rem] text-gray-500 mt-1">{viewingOrder.addressSnapshot.country}</p>
                  </div>
                  <div className="bg-gray-50 px-4 py-3 rounded border border-gray-100 whitespace-nowrap">
                    <p className="text-[0.70rem] font-medium text-gray-500 uppercase mb-1">Contact</p>
                    <a href={`tel:${viewingOrder.addressSnapshot.phoneNumber}`} className="text-gray-800 font-medium text-[0.85rem] hover:text-blue-600 transition-colors flex items-center gap-1">
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-3.5 w-3.5 text-gray-400" viewBox="0 0 20 20" fill="currentColor">
                        <path d="M2 3a1 1 0 011-1h2.153a1 1 0 01.986.836l.74 4.435a1 1 0 01-.54 1.06l-1.548.773a11.037 11.037 0 006.105 6.105l.774-1.548a1 1 0 011.059-.54l4.435.74a1 1 0 01.836.986V17a1 1 0 01-1 1h-2C7.82 18 2 12.18 2 5V3z" />
                      </svg>
                      {viewingOrder.addressSnapshot.phoneNumber}
                    </a>
                  </div>
                </div>
              ) : (
                <div className="mb-6 p-4 bg-gray-50 rounded border border-gray-100 text-gray-500 text-[0.85rem] italic">
                  Shipping address was not recorded for this order.
                </div>
              )}

              <h3 className="text-[0.9rem] font-medium text-gray-800 mb-4 flex items-center border-b border-gray-100 pb-2">
                <Package className="w-4 h-4 mr-2 text-gray-400" /> Ordered Items
              </h3>
              
              <div className="space-y-3">
                {viewingOrder.items && viewingOrder.items.map((item, index) => {
                  const productData = item.productSnapshot || item.product || {}; 

                  return (
                  <div key={index} className="flex flex-col sm:flex-row sm:items-center justify-between p-3 bg-white rounded border border-gray-100 shadow-sm">
                    <Link 
                      to={`/admin/products/${productData.slug || productData.id || productData._id}`}
                      className="flex items-start sm:items-center gap-4 mb-3 sm:mb-0 cursor-pointer group block"
                      title="Click to view product details"
                    >
                      {productData.imageUrls && productData.imageUrls[0] ? (
                        <img src={productData.imageUrls[0]} alt={productData.name} className="w-14 h-14 object-cover rounded border border-gray-100 bg-gray-50 group-hover:border-blue-300 transition-colors" />
                      ) : (
                        <div className="w-14 h-14 bg-gray-50 rounded flex items-center justify-center text-[0.70rem] text-gray-400 border border-gray-100">No Img</div>
                      )}
                      <div>
                        <p className="font-medium text-gray-800 text-[0.85rem] flex items-center gap-2 group-hover:text-blue-600 transition-colors">
                          {productData.name || 'Unknown Product'}
                          <ExternalLink className="w-3.5 h-3.5 opacity-0 group-hover:opacity-100 transition-opacity text-blue-600" />
                        </p>
                        <p className="text-[0.75rem] text-gray-500 font-mono mt-0.5">Code: {productData.productCode || 'N/A'}</p>
                        
                        {productData.specs && productData.specs.length > 0 && (
                          <div className="flex flex-wrap gap-1 mt-1.5">
                            {productData.specs.map((spec, i) => (
                              <span key={i} className="px-1.5 py-0.5 text-[0.65rem] font-medium text-gray-600 bg-gray-100 rounded border border-gray-200">
                                {spec.key}: {spec.value}
                              </span>
                            ))}
                          </div>
                        )}
                      </div>
                    </Link>
                    
                    <div className="text-right sm:text-center sm:ml-4 bg-gray-50 px-3 py-2 rounded border border-gray-100">
                      <p className="text-[0.70rem] text-gray-500 font-medium uppercase mb-0.5">Subtotal</p>
                      <p className="text-[0.9rem] font-semibold text-gray-900">₹{((productData.sellingPrice || productData.mrp || 0) * item.quantity).toLocaleString('en-IN')}</p>
                      <p className="text-[0.70rem] text-gray-400 font-medium">x{item.quantity} units</p>
                    </div>
                  </div>
                )})}
              </div>

              {/* Price Summary */}
              <div className="mt-6 pt-4 border-t border-gray-100">
                <div className="flex flex-col items-end space-y-1.5">
                  <div className="flex justify-between w-full max-w-xs text-[0.85rem]">
                    <span className="text-gray-500">Items Subtotal:</span>
                    <span className="font-medium text-gray-800">₹{viewingOrder.items?.reduce((acc, item) => acc + ((item.productSnapshot?.sellingPrice || item.productSnapshot?.mrp || 0) * item.quantity), 0).toLocaleString('en-IN')}</span>
                  </div>
                  <div className="flex justify-between w-full max-w-xs text-[0.85rem]">
                    <span className="text-gray-500">Delivery Charge:</span>
                    <span className="font-medium text-gray-800">₹{viewingOrder.deliveryCharge?.toLocaleString('en-IN') || 0}</span>
                  </div>
                  {viewingOrder.discountAmount > 0 && (
                    <div className="flex justify-between w-full max-w-xs text-[0.85rem] text-emerald-600 font-medium">
                      <span>Discount {viewingOrder.couponCode ? `(${viewingOrder.couponCode})` : ''}:</span>
                      <span className="font-medium">-₹{viewingOrder.discountAmount.toLocaleString('en-IN')}</span>
                    </div>
                  )}
                  <div className="flex justify-between w-full max-w-xs pt-3 mt-1 border-t border-gray-100">
                    <span className="text-[0.95rem] font-semibold text-gray-800">Grand Total:</span>
                    <span className="text-lg font-bold text-gray-900">
                      ₹{(
                        viewingOrder.items?.reduce((acc, item) => acc + ((item.productSnapshot?.sellingPrice || item.productSnapshot?.mrp || 0) * item.quantity), 0) 
                        + (viewingOrder.deliveryCharge || 0) 
                        - (viewingOrder.discountAmount || 0)
                      ).toLocaleString('en-IN')}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <div className="p-4 border-t border-gray-100 bg-gray-50 flex flex-col gap-3 sm:flex-row sm:justify-between sm:items-center rounded-b">
              <div className="flex items-center gap-2">
                <span className="text-[0.85rem] font-medium text-gray-600">Update Status:</span>
                <select 
                  value={viewingOrder.status} 
                  onChange={(e) => {
                    updateOrderStatus(viewingOrder.id || viewingOrder._id, e.target.value);
                    setViewingOrder({...viewingOrder, status: e.target.value});
                  }}
                  disabled={viewingOrder.status === 'CANCELLED' || viewingOrder.status === 'DELIVERED'}
                  className={`bg-white text-[0.80rem] font-medium px-2 py-1.5 rounded border border-gray-200 focus:ring-0 focus:border-blue-400 outline-none ${getStatusColor(viewingOrder.status)} ${(viewingOrder.status === 'CANCELLED' || viewingOrder.status === 'DELIVERED') ? 'opacity-70 cursor-not-allowed' : 'cursor-pointer'}`}
                >
                  <option value="CREATED">CREATED</option>
                  <option value="CONFIRMED">CONFIRMED</option>
                  <option value="PROCESSING">PROCESSING</option>
                  <option value="SHIPPED">SHIPPED</option>
                  <option value="OUT_FOR_DELIVERY">OUT FOR DELIVERY</option>
                  <option value="DELIVERED">DELIVERED</option>
                </select>
              </div>
              <div className="flex gap-2 self-end sm:self-auto">
                <button
                  onClick={() => handleDownloadReceipt(viewingOrder.id || viewingOrder._id)}
                  disabled={downloadingReceiptId === (viewingOrder.id || viewingOrder._id)}
                  className={`px-4 py-2 border rounded text-[0.85rem] font-medium transition-colors flex items-center gap-1 shadow-sm ${
                    downloadingReceiptId === (viewingOrder.id || viewingOrder._id)
                      ? 'bg-gray-50 border-gray-200 text-gray-400 cursor-not-allowed'
                      : 'bg-white text-emerald-600 border-emerald-200 hover:bg-emerald-50'
                  }`}
                >
                  <Download className="w-4 h-4" />
                  {downloadingReceiptId === (viewingOrder.id || viewingOrder._id)
                    ? 'Downloading...'
                    : 'Receipt'}
                </button>
                <button onClick={() => setViewingOrder(null)} className="px-4 py-2 bg-white text-gray-600 border border-gray-200 rounded text-[0.85rem] font-medium hover:bg-gray-50 transition-colors shadow-sm">
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
