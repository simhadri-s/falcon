import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Search, FilterX, ArrowUpDown, ArrowUp, ArrowDown, Eye, CheckCircle, XCircle, Clock, RefreshCw } from 'lucide-react';
import api from '../services/api';

export default function AdminReturns() {
  const [returns, setReturns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Search, Filter & Sort States
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [sortConfig, setSortConfig] = useState({ key: 'createdAt', direction: 'desc' });

  // Pagination
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(10);

  const fetchReturns = async () => {
    setLoading(true);
    try {
      // For now, let's just fetch all and filter client-side if needed, 
      // but ideally backend should handle pagination and filtering.
      const response = await api.get('/api/returns');
      let data = response.data || [];
      
      if (searchTerm) {
        data = data.filter(r => 
          r.id.toLowerCase().includes(searchTerm.toLowerCase()) || 
          r.orderId.toLowerCase().includes(searchTerm.toLowerCase()) ||
          r.userId.toLowerCase().includes(searchTerm.toLowerCase())
        );
      }
      
      if (statusFilter) {
        data = data.filter(r => r.status === statusFilter);
      }
      
      if (sortConfig.key) {
        data.sort((a, b) => {
          const valA = a[sortConfig.key];
          const valB = b[sortConfig.key];
          if (sortConfig.direction === 'asc') return valA > valB ? 1 : -1;
          return valA < valB ? 1 : -1;
        });
      }
      
      setReturns(data);
    } catch (err) {
      setError("Failed to fetch return requests.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReturns();
  }, [searchTerm, statusFilter, sortConfig]);

  const updateReturnStatus = async (returnId, newStatus) => {
    try {
      await api.patch(`/api/returns/${returnId}/status?status=${newStatus}`);
      fetchReturns();
    } catch (err) {
      alert("Failed to update return status.");
    }
  };

  const getStatusColor = (status) => {
    switch (status?.toUpperCase()) {
      case 'RETURN_REQUESTED': return 'bg-orange-100 text-orange-800 border-orange-200';
      case 'RETURN_APPROVED': return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'RETURN_REJECTED': return 'bg-red-100 text-red-800 border-red-200';
      case 'RETURN_PICKED_UP': return 'bg-purple-100 text-purple-800 border-purple-200';
      case 'RETURN_COMPLETED': return 'bg-green-100 text-green-800 border-green-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const handleSort = (key) => {
    if (sortConfig.key === key) {
      setSortConfig({ key, direction: sortConfig.direction === 'asc' ? 'desc' : 'asc' });
    } else {
      setSortConfig({ key, direction: 'desc' });
    }
  };

  return (
    <div className="relative flex flex-col h-full pb-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-xl font-semibold text-gray-800">Return Management</h1>
          <p className="text-[0.85rem] text-gray-500 mt-1">Approve, reject and track product returns.</p>
        </div>
        <button onClick={fetchReturns} className="flex items-center gap-2 px-3 py-1.5 bg-white border border-gray-200 hover:bg-gray-50 rounded text-gray-700 font-medium text-[0.85rem] transition-colors shadow-sm">
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      {/* SEARCH & FILTER BAR */}
      <div className="bg-white p-4 rounded border border-gray-100 shadow-sm mb-4 flex flex-col md:flex-row gap-4 items-center">
        <div className="relative flex-1 w-full max-w-md">
          <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none"><Search className="w-4 h-4 text-gray-400" /></div>
          <input 
            type="text" 
            placeholder="Search by ID, Order ID or User..." 
            value={searchTerm} 
            onChange={(e) => setSearchTerm(e.target.value)} 
            className="block w-full px-3 py-2 pl-9 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none" 
          />
        </div>
        
        <div className="w-full md:w-56">
          <select 
            value={statusFilter} 
            onChange={(e) => setStatusFilter(e.target.value)} 
            className="block w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white font-medium text-gray-700"
          >
            <option value="">All Statuses</option>
            <option value="RETURN_REQUESTED">REQUESTED</option>
            <option value="RETURN_APPROVED">APPROVED</option>
            <option value="RETURN_REJECTED">REJECTED</option>
            <option value="RETURN_PICKED_UP">PICKED UP</option>
            <option value="RETURN_COMPLETED">COMPLETED</option>
          </select>
        </div>

        {(searchTerm || statusFilter) && <button onClick={() => { setSearchTerm(''); setStatusFilter(''); }} className="px-3 py-1.5 text-gray-500 bg-gray-50 border border-gray-200 hover:text-red-500 hover:bg-gray-100 rounded text-[0.85rem] flex items-center font-medium transition-colors"><FilterX className="w-4 h-4 mr-1.5" /> Clear</button>}
      </div>

      {error && <p className="mb-4 text-red-500 bg-red-50 p-3 rounded">{error}</p>}

      <div className="bg-white rounded border border-gray-100 shadow-sm overflow-hidden flex flex-col flex-1">
        <div className="overflow-x-auto flex-1">
          <table className="w-full text-left border-collapse">
            <thead className="bg-gray-50 border-b border-gray-100">
              <tr>
                <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider cursor-pointer" onClick={() => handleSort('id')}>Return ID</th>
                <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider">Order</th>
                <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider">Customer</th>
                <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider cursor-pointer" onClick={() => handleSort('createdAt')}>Date</th>
                <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider">Status</th>
                <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider text-center">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan="6" className="p-8 text-center text-gray-500 text-[0.85rem]">Loading returns...</td></tr>
              ) : returns.length === 0 ? (
                <tr><td colSpan="6" className="p-8 text-center text-gray-500 text-[0.85rem]">No returns found.</td></tr>
              ) : (
                returns.map((ret) => (
                  <tr key={ret.id} className="hover:bg-gray-50 border-b border-gray-50 transition-colors">
                    <td className="px-4 py-3 font-mono text-[0.80rem] font-medium text-gray-500">
                      {ret.id.slice(-8).toUpperCase()}
                    </td>
                    <td className="px-4 py-3">
                      <Link to={`/admin/orders/${ret.orderId}`} className="text-blue-600 hover:text-blue-700 hover:underline font-medium text-[0.85rem]">
                        #{ret.orderId.slice(-8).toUpperCase()}
                      </Link>
                    </td>
                    <td className="px-4 py-3 font-medium text-gray-800 text-[0.85rem]">{ret.userId}</td>
                    <td className="px-4 py-3 text-[0.85rem] text-gray-600">{new Date(ret.createdAt).toLocaleString()}</td>
                    <td className="px-4 py-3">
                      <select 
                        value={ret.status} 
                        onChange={(e) => updateReturnStatus(ret.id, e.target.value)}
                        className={`text-[0.75rem] font-medium border rounded px-2 py-1 focus:ring-0 focus:border-blue-400 outline-none bg-white ${getStatusColor(ret.status)}`}
                      >
                        <option value="RETURN_REQUESTED">REQUESTED</option>
                        <option value="RETURN_APPROVED">APPROVED</option>
                        <option value="RETURN_REJECTED">REJECTED</option>
                        <option value="RETURN_PICKED_UP">PICKED UP</option>
                        <option value="RETURN_COMPLETED">COMPLETED</option>
                      </select>
                    </td>
                    <td className="px-4 py-3 text-center">
                      <Link to={`/admin/returns/${ret.id}`} className="p-1.5 text-gray-500 hover:text-blue-600 hover:bg-blue-50 rounded inline-block transition-colors">
                        <Eye className="w-4 h-4" />
                      </Link>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
