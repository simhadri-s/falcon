import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Search, FilterX, ArrowUpDown, ArrowUp, ArrowDown, DollarSign, CheckCircle, Clock, XCircle, RefreshCw } from 'lucide-react';
import api from '../services/api';

export default function AdminRefunds() {
  const [refunds, setRefunds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Search, Filter & Sort States
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [sortConfig, setSortConfig] = useState({ key: 'createdAt', direction: 'desc' });

  const fetchRefunds = async () => {
    setLoading(true);
    try {
      const response = await api.get('/api/refunds');
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
      
      setRefunds(data);
    } catch (err) {
      setError("Failed to fetch refunds.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRefunds();
  }, [searchTerm, statusFilter]);

  const updateRefundStatus = async (refundId, newStatus) => {
    try {
      await api.patch(`/api/refunds/${refundId}/status?status=${newStatus}`);
      fetchRefunds();
    } catch (err) {
      alert("Failed to update refund status.");
    }
  };

  const getStatusColor = (status) => {
    switch (status?.toUpperCase()) {
      case 'REFUND_PENDING': return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'REFUND_INITIATED': return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'REFUND_COMPLETED': return 'bg-green-100 text-green-800 border-green-200';
      case 'REFUND_FAILED': return 'bg-red-100 text-red-800 border-red-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  return (
    <div className="relative flex flex-col h-full pb-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-xl font-semibold text-gray-800">Refund Management</h1>
          <p className="text-[0.85rem] text-gray-500 mt-1">Track and process customer refunds.</p>
        </div>
        <div className="bg-white px-4 py-2 rounded border border-gray-100 shadow-sm flex items-center gap-2">
            <span className="text-[0.80rem] font-medium text-gray-500 uppercase tracking-wider">Total Pending:</span>
            <span className="text-lg font-semibold text-orange-600">
                ₹{refunds.filter(r => r.status === 'REFUND_PENDING').reduce((acc, r) => acc + r.amount, 0).toLocaleString('en-IN')}
            </span>
        </div>
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
            <option value="REFUND_PENDING">PENDING</option>
            <option value="REFUND_INITIATED">INITIATED</option>
            <option value="REFUND_COMPLETED">COMPLETED</option>
            <option value="REFUND_FAILED">FAILED</option>
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
                <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider">Refund ID</th>
                <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider">Order</th>
                <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider">Amount</th>
                <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider">Method</th>
                <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider">Status</th>
                <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider">Date</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan="6" className="p-8 text-center text-gray-500 text-[0.85rem]">Loading refunds...</td></tr>
              ) : refunds.length === 0 ? (
                <tr><td colSpan="6" className="p-8 text-center text-gray-500 text-[0.85rem]">No refunds found.</td></tr>
              ) : (
                refunds.map((ref) => (
                  <tr key={ref.id} className="hover:bg-gray-50 border-b border-gray-50 transition-colors">
                    <td className="px-4 py-3 font-mono text-[0.80rem] font-medium text-gray-500">
                      {ref.id.slice(-8).toUpperCase()}
                    </td>
                    <td className="px-4 py-3 font-medium text-[0.85rem] text-blue-600">
                      #{ref.orderId.slice(-8).toUpperCase()}
                    </td>
                    <td className="px-4 py-3 font-semibold text-gray-800 text-[0.90rem]">₹{ref.amount.toLocaleString('en-IN')}</td>
                    <td className="px-4 py-3 text-[0.75rem] font-medium uppercase text-gray-500">{ref.method.replace('_', ' ')}</td>
                    <td className="px-4 py-3">
                      <select 
                        value={ref.status} 
                        onChange={(e) => updateRefundStatus(ref.id, e.target.value)}
                        className={`text-[0.75rem] font-medium border rounded px-2 py-1 focus:ring-0 focus:border-blue-400 outline-none bg-white ${getStatusColor(ref.status)}`}
                      >
                        <option value="REFUND_PENDING">PENDING</option>
                        <option value="REFUND_INITIATED">INITIATED</option>
                        <option value="REFUND_COMPLETED">COMPLETED</option>
                        <option value="REFUND_FAILED">FAILED</option>
                      </select>
                    </td>
                    <td className="px-4 py-3 text-[0.85rem] text-gray-600">{new Date(ref.createdAt).toLocaleDateString()}</td>
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
