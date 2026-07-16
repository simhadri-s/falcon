import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { ArrowLeft, Package, MapPin, Calendar, Mail, UserCircle, ExternalLink, Download } from 'lucide-react';
import api from '../services/api';
import { downloadOrderReceipt } from '../services/orderReceipts';

export default function AdminOrderDetail() {
  const { id } = useParams(); // URL ನಿಂದ ಆರ್ಡರ್ ID ಪಡೆಯುವುದು
  const navigate = useNavigate();
  
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [isUpdating, setIsUpdating] = useState(false);
  const [isDownloadingReceipt, setIsDownloadingReceipt] = useState(false);

  const fetchOrderDetails = async () => {
    setLoading(true);
    try {
      const response = await api.get(`/api/orders/${id}`);
      setOrder(response.data.data || response.data);
    } catch (err) {
      try {
        const allOrdersRes = await api.get('/api/orders');
        const ordersList = allOrdersRes.data.data || allOrdersRes.data || [];
        const foundOrder = ordersList.find(o => (o.id || o._id) === id);
        if (foundOrder) setOrder(foundOrder);
        else setError("Order not found.");
      } catch (fallbackErr) {
        setError("Failed to load order details.");
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (id) fetchOrderDetails();
  }, [id]);

  const updateOrderStatus = async (newStatus) => {
    setIsUpdating(true);
    try {
      const response = await api.put(`/api/orders/${id}/status`, { orderStatus: newStatus });
      setOrder(response.data); // ಹೊಸ ಸ್ಟೇಟಸ್ ಅಪ್‌ಡೇಟ್ ಮಾಡುವುದು
      alert("Status updated successfully!");
    } catch (err) {
      alert("Failed to update status.");
    } finally {
      setIsUpdating(false);
    }
  };

  const cancelOrder = async () => {
    if (window.confirm("Are you sure you want to completely cancel this order?")) {
      setIsUpdating(true);
      try {
        await api.put(`/api/orders/${id}/cancel`);
        setOrder({ ...order, status: 'CANCELLED' });
      } catch (err) {
        alert(err.response?.data?.message || "Failed to cancel order.");
      } finally {
        setIsUpdating(false);
      }
    }
  };

  const handleDownloadReceipt = async () => {
    const orderId = order?.id || order?._id || id;
    if (!orderId || isDownloadingReceipt) return;

    setIsDownloadingReceipt(true);
    try {
      await downloadOrderReceipt(orderId);
    } catch (err) {
      alert(err.message || 'Failed to download receipt.');
    } finally {
      setIsDownloadingReceipt(false);
    }
  };

  const getStatusColor = (status) => {
    switch (status?.toUpperCase()) {
      case 'CREATED': return 'text-blue-700 bg-blue-100 border-blue-200';
      case 'CONFIRMED': return 'text-teal-700 bg-teal-100 border-teal-200';
      case 'PROCESSING': return 'text-yellow-700 bg-yellow-100 border-yellow-200';
      case 'SHIPPED': return 'text-purple-700 bg-purple-100 border-purple-200';
      case 'OUT_FOR_DELIVERY': return 'text-orange-700 bg-orange-100 border-orange-200';
      case 'DELIVERED': return 'text-green-700 bg-green-100 border-green-200';
      case 'CANCELLED': return 'text-red-700 bg-red-100 border-red-200';
      default: return 'text-gray-700 bg-gray-100 border-gray-200';
    }
  };

  if (loading) return <div className="p-8 text-center text-gray-500 font-bold text-lg mt-20">Loading Order Details...</div>;
  if (error) return <div className="p-8 text-center text-red-500 font-bold text-lg mt-20">{error}</div>;
  if (!order) return <div className="p-8 text-center text-gray-500 mt-20">No data found.</div>;

  const addr = order.addressSnapshot;

  return (
    <div className="relative flex flex-col h-full pb-10 max-w-6xl mx-auto">
      
      {/* --- Header Section --- */}
      <div className="flex flex-col md:flex-row md:items-center justify-between mb-6 gap-4">
        <div className="flex items-center gap-4">
          <button onClick={() => navigate(-1)} className="p-2 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition shadow-sm">
            <ArrowLeft className="w-5 h-5 text-gray-600" />
          </button>
          <div>
            <h1 className="text-2xl font-black text-[#1E3A5F] flex items-center gap-3">
              Order {order.id || order._id}
              <span className={`px-3 py-1 text-xs font-bold rounded-full border ${getStatusColor(order.status)}`}>
                {order.status}
              </span>
            </h1>
            <p className="text-gray-500 text-sm mt-1 flex items-center gap-1">
              <Calendar className="w-4 h-4" /> Placed on {new Date(order.createdAt).toLocaleString('en-US', { dateStyle: 'full', timeStyle: 'short' })}
            </p>
          </div>
        </div>

        <div className="flex flex-wrap gap-3 items-center bg-white p-2 rounded-lg border shadow-sm">
          <button
            onClick={handleDownloadReceipt}
            disabled={isDownloadingReceipt}
            className={`flex items-center gap-2 px-4 py-2 text-sm font-bold rounded transition ${
              isDownloadingReceipt
                ? 'bg-gray-100 text-gray-400 border border-gray-200 cursor-not-allowed'
                : 'text-[#1E3A5F] bg-blue-50 hover:bg-blue-100 border border-blue-200'
            }`}
            title="Download PDF receipt"
          >
            <Download className="w-4 h-4" />
            {isDownloadingReceipt ? 'Downloading...' : 'Download Receipt'}
          </button>
          <span className="text-sm font-bold text-gray-500 ml-2">Update Status:</span>
          <select 
            value={order.status} 
            onChange={(e) => updateOrderStatus(e.target.value)}
            disabled={isUpdating || order.status === 'CANCELLED' || order.status === 'DELIVERED'}
            className="text-sm font-bold border rounded p-2 focus:ring-[#1E3A5F] outline-none bg-gray-50 cursor-pointer"
          >
            <option value="CREATED">CREATED</option>
            <option value="CONFIRMED">CONFIRMED</option>
            <option value="PROCESSING">PROCESSING</option>
            <option value="SHIPPED">SHIPPED</option>
            <option value="OUT_FOR_DELIVERY">OUT FOR DELIVERY</option>
            <option value="DELIVERED">DELIVERED</option>
          </select>
          {order.status !== 'CANCELLED' && order.status !== 'DELIVERED' && (
            <button 
              onClick={cancelOrder} 
              disabled={isUpdating}
              className="px-4 py-2 text-sm font-bold text-red-600 bg-red-50 hover:bg-red-100 border border-red-200 rounded transition"
            >
              Cancel Order
            </button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* --- Left Column (Customer & Shipping) --- */}
        <div className="lg:col-span-1 space-y-6">
          
          {/* Customer Info Card */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
            <div className="p-4 border-b bg-gray-50 flex items-center gap-2">
              <UserCircle className="w-5 h-5 text-[#E8A020]" />
              <h2 className="font-bold text-[#1E3A5F]">Customer Information</h2>
            </div>
            <div className="p-5">
              <p className="flex items-center gap-3 text-gray-700 font-medium">
                <Mail className="w-5 h-5 text-gray-400" />
                {order.userId}
              </p>
            </div>
          </div>

          {/* Shipping Address Card */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
            <div className="p-4 border-b bg-gray-50 flex items-center gap-2">
              <MapPin className="w-5 h-5 text-[#E8A020]" />
              <h2 className="font-bold text-[#1E3A5F]">Shipping Address</h2>
            </div>
            <div className="p-5">
              {addr ? (
                <div>
                  <p className="font-black text-gray-900 text-lg mb-2">{addr.fullName}</p>
                  <p className="text-gray-600 leading-relaxed mb-1">{addr.street}</p>
                  <p className="text-gray-600 mb-2">{addr.city} - <span className="font-bold text-gray-800">{addr.pincode}</span></p>
                  <p className="text-sm text-gray-400 font-bold uppercase tracking-wider mb-4">{addr.country}</p>
                  
                  <div className="pt-4 border-t border-gray-100">
                    <p className="text-xs font-bold text-gray-400 uppercase mb-1">Contact Number</p>
                    <a href={`tel:${addr.phoneNumber}`} className="text-[#1E3A5F] font-black text-lg hover:underline">
                      {addr.phoneNumber}
                    </a>
                  </div>
                </div>
              ) : (
                <p className="text-gray-500 italic">No shipping details provided.</p>
              )}
            </div>
          </div>
        </div>

        {/* --- Right Column (Ordered Items) --- */}
        <div className="lg:col-span-2">
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
            <div className="p-4 border-b bg-gray-50 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Package className="w-5 h-5 text-[#E8A020]" />
                <h2 className="font-bold text-[#1E3A5F]">Ordered Items</h2>
              </div>
              <span className="bg-[#1E3A5F] text-white text-xs font-bold px-3 py-1 rounded-full">
                {order.items?.length || 0} Items
              </span>
            </div>
            
            <div className="p-0">
              <table className="w-full text-left border-collapse">
                <thead className="bg-white border-b text-xs uppercase text-gray-400 font-black">
                  <tr>
                    <th className="p-4">Product Details</th>
                    <th className="p-4 text-center">Quantity</th>
                    <th className="p-4 text-right">Subtotal</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {order.items && order.items.map((item, index) => {
                    const pData = item.productSnapshot || item.product || {}; 
                    const itemPrice = pData.sellingPrice || pData.mrp || 0;
                    
                    return (
                      <tr key={index} className="hover:bg-gray-50 transition-colors">
                        <td className="p-4">
                          <Link 
                            to={`/admin/products/${pData.slug || pData.id || pData._id}`}
                            className="flex items-start gap-4 cursor-pointer group block"
                          >
                            {pData.imageUrls && pData.imageUrls[0] ? (
                              <img src={pData.imageUrls[0]} alt={pData.name} className="w-16 h-16 object-cover rounded-lg border shadow-sm" />
                            ) : (
                              <div className="w-16 h-16 bg-gray-100 rounded-lg flex items-center justify-center text-xs text-gray-400 border">No Img</div>
                            )}
                            <div>
                              <p className="font-bold text-[#1E3A5F] group-hover:text-blue-600 group-hover:underline transition-all">
                                {pData.name || 'Unknown Product'}
                              </p>
                              <p className="text-xs text-gray-500 font-mono mt-1">Code: {pData.productCode || 'N/A'}</p>
                              <p className="text-xs font-bold text-gray-700 mt-1">₹{itemPrice.toLocaleString('en-IN')}</p>
                            </div>
                          </Link>
                        </td>
                        <td className="p-4 text-center align-middle">
                          <span className="text-xl font-black text-[#E8A020]">x{item.quantity}</span>
                        </td>
                        <td className="p-4 text-right align-middle font-bold text-gray-800">
                          ₹{(itemPrice * item.quantity).toLocaleString('en-IN')}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            
            {/* Price Summary */}
            <div className="p-6 bg-gray-50 border-t flex justify-end">
              <div className="w-full max-w-xs space-y-3">
                <div className="flex justify-between text-sm text-gray-600">
                  <span>Items Subtotal</span>
                  <span className="font-bold">₹{order.items?.reduce((acc, item) => acc + ((item.productSnapshot?.sellingPrice || item.productSnapshot?.mrp || 0) * item.quantity), 0).toLocaleString('en-IN')}</span>
                </div>
                <div className="flex justify-between text-sm text-gray-600">
                  <span>Delivery Charge</span>
                  <span className="font-bold">₹{order.deliveryCharge?.toLocaleString('en-IN') || 0}</span>
                </div>
                {order.discountAmount > 0 && (
                  <div className="flex justify-between text-sm text-green-600 font-medium">
                    <span>Discount {order.couponCode ? `(${order.couponCode})` : ''}</span>
                    <span className="font-bold">-₹{order.discountAmount.toLocaleString('en-IN')}</span>
                  </div>
                )}
                <div className="pt-3 border-t border-gray-200 flex justify-between items-center">
                  <span className="text-lg font-bold text-[#1E3A5F]">Total Amount</span>
                  <span className="text-2xl font-black text-[#E8A020]">
                    ₹{(
                      order.items?.reduce((acc, item) => acc + ((item.productSnapshot?.sellingPrice || item.productSnapshot?.mrp || 0) * item.quantity), 0) 
                      + (order.deliveryCharge || 0) 
                      - (order.discountAmount || 0)
                    ).toLocaleString('en-IN')}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>
  );
}
