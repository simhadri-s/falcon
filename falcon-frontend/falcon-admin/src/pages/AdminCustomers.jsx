import { useEffect, useState } from 'react';
import { Search, FilterX, Mail, MapPin, ShoppingBag, Eye, Calendar, Package, UserCircle } from 'lucide-react';
import api from '../services/api';
import { useNavigate } from 'react-router-dom'; 

export default function AdminCustomers() {
  const navigate = useNavigate();
  const [users, setUsers] = useState([]);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Search & Pagination States
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;

  // Modal State
  const [selectedUser, setSelectedUser] = useState(null);

  const fetchData = async () => {
    setLoading(true);
    try {
      // --- UPDATED: Passing the 'search' parameter to the backend ---
      const params = { name: 'USER' };
      if (searchTerm) {
        params.search = searchTerm;
      }

      const [usersRes, ordersRes] = await Promise.all([
        api.get('/api/users/role', { params }), // Backend filtering applied here
        api.get('/api/orders')
      ]);
      
      setUsers(usersRes.data.data || usersRes.data || []);
      setOrders(ordersRes.data.data || ordersRes.data || []);
    } catch (err) {
      console.error(err);
      setError("Failed to load users and their history.");
    } finally {
      setLoading(false);
    }
  };

  // --- NEW: Reset to page 1 when search term changes ---
  useEffect(() => { 
    setCurrentPage(1); 
  }, [searchTerm]);

  // --- NEW: Debounce Search (Waits 500ms after typing before calling API) ---
  useEffect(() => {
    const timer = setTimeout(() => {
      fetchData();
    }, 500);
    
    return () => clearTimeout(timer); // Cleanup timer on consecutive keystrokes
  }, [searchTerm]);


  // Client-side pagination (Backend already filtered the users)
  const totalPages = Math.ceil(users.length / itemsPerPage) || 1;
  const paginatedUsers = users.slice((currentPage - 1) * itemsPerPage, currentPage * itemsPerPage);

  // Helper functions for user stats
  const getUserOrders = (email) => orders.filter(o => o.userId === email);
  
  const extractUserAddresses = (userOrders) => {
    const addresses = [];
    const seenPincodes = new Set();
    
    userOrders.forEach(order => {
      if (order.addressSnapshot && !seenPincodes.has(order.addressSnapshot.pincode + order.addressSnapshot.street)) {
        addresses.push(order.addressSnapshot);
        seenPincodes.add(order.addressSnapshot.pincode + order.addressSnapshot.street);
      }
    });
    return addresses;
  };

  const getStatusColor = (status) => {
    switch (status?.toUpperCase()) {
      case 'CREATED': return 'text-blue-600 bg-blue-100';
      case 'CONFIRMED': return 'text-teal-600 bg-teal-100';
      case 'PROCESSING': return 'text-yellow-600 bg-yellow-100';
      case 'SHIPPED': return 'text-purple-600 bg-purple-100';
      case 'OUT_FOR_DELIVERY': return 'text-orange-600 bg-orange-100';
      case 'DELIVERED': return 'text-green-600 bg-green-100';
      case 'CANCELLED': return 'text-red-600 bg-red-100';
      default: return 'text-gray-600 bg-gray-100';
    }
  };

  return (
    <div className="relative flex flex-col h-full pb-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-xl font-semibold text-gray-800">Customers Directory</h1>
          <p className="text-[0.85rem] text-gray-500 mt-1">View user details, lifetime orders, and shipping addresses.</p>
        </div>
      </div>

      <div className="bg-white p-4 rounded border border-gray-100 shadow-sm mb-4 flex items-center">
        <div className="relative flex-1 max-w-md">
          <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none"><Search className="w-4 h-4 text-gray-400" /></div>
          <input 
            type="text" 
            placeholder="Search customer by email (Server-side)..." 
            value={searchTerm} 
            onChange={(e) => setSearchTerm(e.target.value)} 
            className="block w-full px-3 py-2 pl-9 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none" 
          />
        </div>
        {searchTerm && <button onClick={() => setSearchTerm('')} className="ml-3 px-3 py-1.5 text-gray-500 bg-gray-50 border border-gray-200 hover:text-red-500 hover:bg-gray-100 rounded text-[0.85rem] flex items-center font-medium transition-colors"><FilterX className="w-4 h-4 mr-1.5" /> Clear</button>}
      </div>

      {error && <p className="mb-4 text-red-500 bg-red-50 p-3 rounded">{error}</p>}

      <div className="overflow-hidden bg-white rounded border border-gray-100 shadow-sm flex flex-col flex-1">
        <div className="overflow-x-auto flex-1">
          <table className="w-full text-left border-collapse min-w-[800px]">
            <thead className="bg-gray-50 border-b border-gray-100 sticky top-0 z-10 select-none">
              <tr>
                <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider w-16 text-center">User</th>
                <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider">Email Address</th>
                <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider text-center">Total Orders</th>
                <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider text-center">Lifetime Items Bought</th>
                <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider w-32 text-center">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan="5" className="p-8 text-center text-gray-500 text-[0.85rem]">Searching customers...</td></tr>
              ) : paginatedUsers.length === 0 ? (
                <tr><td colSpan="5" className="p-8 text-center text-gray-500 text-[0.85rem]">No customers found.</td></tr>
              ) : (
                paginatedUsers.map((user) => {
                  const userOrders = getUserOrders(user.email);
                  const totalItems = userOrders.reduce((sum, order) => sum + (order.items?.length || 0), 0);

                  return (
                    <tr key={user.id || user._id} className="hover:bg-gray-50 border-b border-gray-50 transition-colors">
                      <td className="px-4 py-3 text-center text-gray-400"><UserCircle className="w-6 h-6 mx-auto opacity-70" /></td>
                      <td className="px-4 py-3 font-medium text-gray-800 text-[0.85rem]">{user.email}</td>
                      <td className="px-4 py-3 text-center">
                        <span className="bg-blue-50 text-blue-700 text-[0.75rem] font-medium px-2 py-0.5 rounded border border-blue-100">
                          {userOrders.length}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-center text-[0.85rem] text-gray-600">{totalItems} Items</td>
                      <td className="px-4 py-3 text-center">
                        <button 
                          onClick={() => setSelectedUser({ ...user, history: userOrders })} 
                          className="px-3 py-1.5 text-[0.80rem] font-medium text-gray-600 bg-white border border-gray-200 rounded hover:bg-gray-50 flex items-center justify-center mx-auto transition-colors"
                        >
                          <Eye className="w-3.5 h-3.5 mr-1.5" /> View Profile
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {!loading && users.length > 0 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-gray-50 mt-auto">
            <span className="text-[0.85rem] text-gray-600">Page <span className="font-medium text-gray-800">{currentPage}</span> of <span className="font-medium text-gray-800">{totalPages}</span></span>
            <div className="flex space-x-1">
              <button onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))} disabled={currentPage === 1} className={`px-3 py-1.5 rounded text-[0.80rem] font-medium border ${currentPage === 1 ? 'bg-gray-50 text-gray-400 border-gray-100 cursor-not-allowed' : 'bg-white text-gray-600 border-gray-200 hover:bg-gray-100'}`}>Prev</button>
              <button onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))} disabled={currentPage === totalPages} className={`px-3 py-1.5 rounded text-[0.80rem] font-medium border ${currentPage === totalPages ? 'bg-gray-50 text-gray-400 border-gray-100 cursor-not-allowed' : 'bg-white text-gray-600 border-gray-200 hover:bg-gray-100'}`}>Next</button>
            </div>
          </div>
        )}
      </div>

      {/* USER PROFILE MODAL */}
      {selectedUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="w-full max-w-4xl bg-white rounded shadow-2xl overflow-hidden flex flex-col max-h-[95vh]">
            
            <div className="p-6 border-b border-gray-100 flex justify-between items-center">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-blue-50 border border-blue-100 flex items-center justify-center">
                  <UserCircle className="w-6 h-6 text-blue-500" />
                </div>
                <div>
                  <h2 className="text-lg font-semibold text-gray-800">{selectedUser.email}</h2>
                  <p className="text-gray-500 text-[0.80rem] flex items-center gap-1 mt-0.5">
                    <Mail className="w-3 h-3" /> Customer Profile
                  </p>
                </div>
              </div>
              <button onClick={() => setSelectedUser(null)} className="text-gray-400 hover:text-red-500 transition-colors">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M18 6L6 18"/><path d="M6 6l12 12"/></svg>
              </button>
            </div>
            
            <div className="p-6 overflow-y-auto flex-1 bg-gray-50/50">
              
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
                <div className="bg-white p-4 rounded border border-gray-100 shadow-sm text-center">
                  <ShoppingBag className="w-5 h-5 mx-auto text-blue-500 mb-2" />
                  <p className="text-[0.70rem] font-medium text-gray-500 uppercase tracking-wider">Total Orders</p>
                  <p className="text-lg font-semibold text-gray-800 mt-1">{selectedUser.history.length}</p>
                </div>
                <div className="bg-white p-4 rounded border border-gray-100 shadow-sm text-center">
                  <Package className="w-5 h-5 mx-auto text-orange-500 mb-2" />
                  <p className="text-[0.70rem] font-medium text-gray-500 uppercase tracking-wider">Items Purchased</p>
                  <p className="text-lg font-semibold text-gray-800 mt-1">
                    {selectedUser.history.reduce((sum, o) => sum + (o.items?.length || 0), 0)}
                  </p>
                </div>
                <div className="bg-white p-4 rounded border border-gray-100 shadow-sm text-center col-span-2 md:col-span-2">
                  <Calendar className="w-5 h-5 mx-auto text-emerald-500 mb-2" />
                  <p className="text-[0.70rem] font-medium text-gray-500 uppercase tracking-wider">First Order Date</p>
                  <p className="text-[0.90rem] font-medium text-gray-800 mt-1">
                    {selectedUser.history.length > 0 
                      ? new Date(selectedUser.history[selectedUser.history.length - 1]?.createdAt).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' }) 
                      : 'No orders yet'}
                  </p>
                </div>
              </div>

              <h3 className="text-[0.90rem] font-semibold text-gray-800 mb-4 flex items-center border-b border-gray-100 pb-2">
                <MapPin className="w-4 h-4 mr-2 text-gray-400" /> Known Shipping Addresses
              </h3>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">
                {extractUserAddresses(selectedUser.history).length > 0 ? (
                  extractUserAddresses(selectedUser.history).map((addr, idx) => (
                    <div key={idx} className="bg-white p-4 rounded border border-gray-100 shadow-sm">
                      <p className="font-semibold text-gray-800 text-[0.90rem] mb-1">{addr.fullName}</p>
                      <p className="text-gray-600 text-[0.85rem] leading-relaxed">{addr.street}</p>
                      <p className="text-gray-600 text-[0.85rem]">{addr.city} - <span className="font-medium text-gray-800">{addr.pincode}</span></p>
                      <p className="text-[0.80rem] text-gray-400 mt-1">{addr.country}</p>
                      <div className="mt-3 pt-3 border-t border-gray-50 flex items-center">
                        <span className="text-[0.70rem] font-medium text-gray-500 uppercase tracking-wider mr-2">Contact:</span>
                        <span className="text-[0.85rem] font-medium text-gray-700">{addr.phoneNumber}</span>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="col-span-2 p-6 bg-gray-50 rounded border border-dashed border-gray-200 text-center text-gray-500 text-[0.85rem] italic">
                    No shipping addresses found for this customer.
                  </div>
                )}
              </div>

              <h3 className="text-[0.90rem] font-semibold text-gray-800 mb-4 flex items-center border-b border-gray-100 pb-2">
                <ShoppingBag className="w-4 h-4 mr-2 text-gray-400" /> Lifetime Order History
              </h3>

              <div className="bg-white rounded border border-gray-100 shadow-sm overflow-hidden">
                {selectedUser.history.length > 0 ? (
                  <table className="w-full text-left">
                    <thead className="bg-gray-50 border-b border-gray-100">
                      <tr>
                        <th className="px-4 py-3 text-[0.70rem] uppercase text-gray-500 font-semibold tracking-wider">Order ID</th>
                        <th className="px-4 py-3 text-[0.70rem] uppercase text-gray-500 font-semibold tracking-wider">Date</th>
                        <th className="px-4 py-3 text-[0.70rem] uppercase text-gray-500 font-semibold tracking-wider text-center">Items</th>
                        <th className="px-4 py-3 text-[0.70rem] uppercase text-gray-500 font-semibold tracking-wider">Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {selectedUser.history.map(order => (
                      <tr 
                        key={order.id || order._id} 
                        onClick={() => navigate(`/admin/orders/${order.id || order._id}`)}
                        className="hover:bg-gray-50 border-b border-gray-50 last:border-b-0 cursor-pointer transition-colors"
                        title="Click to view full order details"
                      >
                        <td className="px-4 py-3 font-mono text-[0.80rem] font-medium text-blue-600 hover:text-blue-700">{(order.id || order._id).substring(0, 8)}...</td>
                        <td className="px-4 py-3 text-[0.85rem] text-gray-700">{new Date(order.createdAt).toLocaleDateString()}</td>
                        <td className="px-4 py-3 text-center text-[0.85rem] font-medium text-gray-800">{order.items?.length || 0}</td>
                        <td className="px-4 py-3">
                          <span className={`px-2 py-0.5 text-[0.70rem] font-medium rounded border ${getStatusColor(order.status)}`}>
                            {order.status}
                          </span>
                        </td>
                      </tr>
                      ))}
                    </tbody>
                  </table>
                ) : (
                  <div className="p-6 text-center text-[0.85rem] text-gray-500 italic">This user hasn't placed any orders yet.</div>
                )}
              </div>

            </div>

            <div className="p-4 border-t border-gray-100 bg-white flex justify-end">
              <button onClick={() => setSelectedUser(null)} className="px-4 py-2 text-[0.85rem] text-gray-700 bg-white border border-gray-200 rounded hover:bg-gray-50 font-medium transition-colors">
                Close Profile
              </button>
            </div>

          </div>
        </div>
      )}
    </div>
  );
}