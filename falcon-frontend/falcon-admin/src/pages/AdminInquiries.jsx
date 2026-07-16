import { useEffect, useState } from 'react';
import { Search, FilterX, ArrowUpDown, ArrowUp, ArrowDown, Eye, Trash2 } from 'lucide-react';
import api from '../services/api';

export default function AdminInquiries() {
  const [inquiries, setInquiries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  // Pagination
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);

  // Search & Sort States
  const [searchTerm, setSearchTerm] = useState('');
  const [sortConfig, setSortConfig] = useState({ key: null, direction: 'asc' });

  // View Modal State
  const [viewingInquiry, setViewingInquiry] = useState(null);

  const fetchInquiries = async () => {
    setLoading(true);
    try {
      const params = {
        page: currentPage,
        limit: 10,
      };
      if (sortConfig.key) {
        params.sortBy = sortConfig.key;
        params.sortDirection = sortConfig.direction;
      }
      const response = await api.get(`/api/inquiries`, { params });
      setInquiries(response.data.data || response.data || []);
      setTotalPages(response.data.pages || 1);
    } catch (err) { setError("Failed to fetch inquiries."); } finally { setLoading(false); }
  };

  useEffect(() => { fetchInquiries(); }, [currentPage, sortConfig]);

  // --- PUT Request to update status ---
  const updateInquiryStatus = async (inquiryId, newStatus) => {
    try {
      // Sends payload {"status": "RESPONDED"} using PUT method
      await api.put(`/api/inquiries/${inquiryId}/status`, { status: newStatus });
      fetchInquiries(); 
      
      // Update modal state if open
      if (viewingInquiry && (viewingInquiry.id || viewingInquiry._id) === inquiryId) {
        setViewingInquiry({ ...viewingInquiry, status: newStatus });
      }
    } catch (err) { alert("Failed to update status."); }
  };

  const handleDeleteClick = async (id) => {
    if(window.confirm("Are you sure you want to delete this inquiry?")) {
      try {
        await api.delete(`/api/inquiries/${id}`);
        fetchInquiries();
      } catch(err) {
        alert("Failed to delete inquiry.");
      }
    }
  };

  // Search & Sort Logic
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
    if (sortConfig.key !== columnName || !sortConfig.key) return <ArrowUpDown className="w-4 h-4 ml-1 opacity-50" />;
    return sortConfig.direction === 'asc' ? <ArrowUp className="w-4 h-4 ml-1" /> : <ArrowDown className="w-4 h-4 ml-1" />;
  };

  const filteredInquiries = inquiries.filter(inq => 
      inq.name?.toLowerCase().includes(searchTerm.toLowerCase()) || 
      inq.email?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      inq.subject?.toLowerCase().includes(searchTerm.toLowerCase())
    );

  // --- UPDATED: 3 Specific Status Colors ---
  const getStatusColor = (status) => {
    switch (status?.toUpperCase()) {
      case 'NEW': return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'READ': return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'RESPONDED': return 'bg-green-100 text-green-800 border-green-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  return (
    <div className="relative flex flex-col h-full pb-6">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-semibold text-gray-800">Customer Inquiries</h1>
      </div>

      <div className="bg-white p-4 rounded border border-gray-100 shadow-sm mb-4 flex items-center">
        <div className="relative flex-1 max-w-md">
          <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none"><Search className="w-4 h-4 text-gray-400" /></div>
          <input 
            type="text" 
            placeholder="Search by name, email, or subject..." 
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
                <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider cursor-pointer hover:bg-gray-100 transition-colors" onClick={() => handleSort('createdAt')}>
                  <div className="flex items-center">Date {getSortIcon('createdAt')}</div>
                </th>
                <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider cursor-pointer hover:bg-gray-100 transition-colors" onClick={() => handleSort('name')}>
                  <div className="flex items-center">Customer {getSortIcon('name')}</div>
                </th>
                <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider cursor-pointer hover:bg-gray-100 transition-colors" onClick={() => handleSort('subject')}>
                  <div className="flex items-center">Subject {getSortIcon('subject')}</div>
                </th>
                <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider cursor-pointer hover:bg-gray-100 transition-colors" onClick={() => handleSort('status')}>
                  <div className="flex items-center">Status {getSortIcon('status')}</div>
                </th>
                <th className="px-4 py-3 text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider w-32">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan="5" className="p-8 text-center text-[0.85rem] text-gray-500">Loading inquiries...</td></tr>
              ) : filteredInquiries.length === 0 ? (
                <tr><td colSpan="5" className="p-8 text-center text-[0.85rem] text-gray-500">No inquiries found.</td></tr>
              ) : (
                filteredInquiries.map((inquiry) => (
                  <tr key={inquiry.id || inquiry._id} className="hover:bg-gray-50 border-b border-gray-50 transition-colors">
                    <td className="px-4 py-3 text-[0.85rem] text-gray-500 whitespace-nowrap">
                      {new Date(inquiry.createdAt).toLocaleDateString()}
                    </td>
                    <td className="px-4 py-3">
                      <div className="font-medium text-[0.85rem] text-gray-800">{inquiry.name}</div>
                      <div className="text-[0.80rem] text-gray-500">{inquiry.email}</div>
                    </td>
                    <td className="px-4 py-3">
                      <div className="font-medium text-[0.85rem] text-gray-800">{inquiry.subject}</div>
                      <div className="text-[0.80rem] text-gray-500 mt-0.5 max-w-xs truncate">{inquiry.message}</div>
                    </td>
                    <td className="px-4 py-3">
                      {/* --- UPDATED: 3 Specific Status Options --- */}
                      <select 
                        value={inquiry.status || 'NEW'} 
                        onChange={(e) => updateInquiryStatus(inquiry.id || inquiry._id, e.target.value)}
                        className={`text-[0.75rem] font-medium border rounded px-2 py-1 focus:ring-0 focus:border-blue-400 outline-none bg-white ${getStatusColor(inquiry.status)}`}
                      >
                        <option value="NEW">NEW</option>
                        <option value="READ">READ</option>
                        <option value="RESPONDED">RESPONDED</option>
                      </select>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex gap-2">
                        <button 
                          onClick={() => setViewingInquiry(inquiry)} 
                          className="p-1.5 text-gray-500 hover:text-blue-600 hover:bg-blue-50 rounded transition-colors"
                          title="View Details"
                        >
                          <Eye className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {!loading && inquiries.length > 0 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-gray-50 mt-auto">
            <span className="text-[0.85rem] text-gray-600">Page <span className="font-medium text-gray-800">{currentPage}</span> of <span className="font-medium text-gray-800">{totalPages}</span></span>
            <div className="flex space-x-1">
              <button onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))} disabled={currentPage === 1} className={`px-3 py-1.5 rounded text-[0.80rem] font-medium border ${currentPage === 1 ? 'bg-gray-50 text-gray-400 border-gray-100 cursor-not-allowed' : 'bg-white text-gray-600 border-gray-200 hover:bg-gray-100'}`}>Prev</button>
              <button onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))} disabled={currentPage === totalPages} className={`px-3 py-1.5 rounded text-[0.80rem] font-medium border ${currentPage === totalPages ? 'bg-gray-50 text-gray-400 border-gray-100 cursor-not-allowed' : 'bg-white text-gray-600 border-gray-200 hover:bg-gray-100'}`}>Next</button>
            </div>
          </div>
        )}
      </div>

      {/* VIEW INQUIRY MODAL */}
      {viewingInquiry && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="w-full max-w-2xl bg-white rounded shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
            <div className="p-6 border-b border-gray-100 flex justify-between items-center">
              <h2 className="text-lg font-semibold text-gray-800">Inquiry Details</h2>
              <button onClick={() => setViewingInquiry(null)} className="text-gray-400 hover:text-red-500 transition-colors">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M18 6L6 18"/><path d="M6 6l12 12"/></svg>
              </button>
            </div>
            
            <div className="p-6 overflow-y-auto flex-1 bg-white">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6 p-4 bg-gray-50 rounded border border-gray-100">
                <div>
                  <p className="text-[0.70rem] font-medium text-gray-500 uppercase tracking-wider mb-1">Customer Name</p>
                  <p className="font-semibold text-gray-800 text-[0.85rem]">{viewingInquiry.name}</p>
                </div>
                <div>
                  <p className="text-[0.70rem] font-medium text-gray-500 uppercase tracking-wider mb-1">Date Submitted</p>
                  <p className="font-medium text-gray-800 text-[0.85rem]">{new Date(viewingInquiry.createdAt).toLocaleString()}</p>
                </div>
                <div>
                  <p className="text-[0.70rem] font-medium text-gray-500 uppercase tracking-wider mb-1">Email Address</p>
                  <a href={`mailto:${viewingInquiry.email}`} className="font-medium text-[0.85rem] text-blue-600 hover:underline">{viewingInquiry.email}</a>
                </div>
                <div>
                  <p className="text-[0.70rem] font-medium text-gray-500 uppercase tracking-wider mb-1">Phone Number</p>
                  <a href={`tel:${viewingInquiry.phone}`} className="font-medium text-[0.85rem] text-blue-600 hover:underline">{viewingInquiry.phone || 'Not provided'}</a>
                </div>
              </div>

              <div className="mb-6">
                <p className="text-[0.70rem] font-medium text-gray-500 uppercase tracking-wider mb-2">Subject</p>
                <p className="font-semibold text-[0.85rem] text-gray-800 p-3 bg-gray-50 rounded border border-gray-100">{viewingInquiry.subject}</p>
              </div>

              <div className="mb-6">
                <p className="text-[0.70rem] font-medium text-gray-500 uppercase tracking-wider mb-2">Full Message</p>
                <div className="p-4 bg-gray-50 rounded border border-gray-100 text-gray-700 text-[0.85rem] whitespace-pre-wrap leading-relaxed">
                  {viewingInquiry.message}
                </div>
              </div>

              <div className="flex items-center gap-3 p-4 border-t border-gray-100 mt-4">
                <label className="font-medium text-gray-700 text-[0.85rem]">Update Status:</label>
                {/* --- UPDATED: 3 Specific Status Options in Modal --- */}
                <select 
                  value={viewingInquiry.status || 'NEW'} 
                  onChange={(e) => updateInquiryStatus(viewingInquiry.id || viewingInquiry._id, e.target.value)}
                  className={`text-[0.75rem] font-medium border rounded px-2 py-1.5 focus:ring-0 focus:border-blue-400 outline-none bg-white ${getStatusColor(viewingInquiry.status)}`}
                >
                  <option value="NEW">NEW</option>
                  <option value="READ">READ</option>
                  <option value="RESPONDED">RESPONDED</option>
                </select>
              </div>
            </div>

            <div className="p-4 border-t border-gray-100 bg-gray-50 flex justify-end">
              <button 
                onClick={() => setViewingInquiry(null)} 
                className="px-4 py-2 bg-white text-gray-600 border border-gray-200 rounded text-[0.85rem] font-medium hover:bg-gray-50 transition-colors shadow-sm"
              >
                Close Window
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}