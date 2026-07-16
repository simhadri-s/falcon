import { useEffect, useState } from 'react';
import { Search, FilterX, Trash2, CheckCircle, XCircle, Star, CalendarDays } from 'lucide-react';
import { Link } from 'react-router-dom';
import api from '../services/api';

export default function AdminReviews() {
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [limit, setLimit] = useState(10);
  
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const [filterRating, setFilterRating] = useState('');

  const fetchReviews = async () => {
    setLoading(true);
    try {
      const params = { page: currentPage, limit };
      if (searchTerm) params.search = searchTerm;
      if (filterStatus) params.status = filterStatus;
      if (filterRating) params.rating = filterRating;
      
      const res = await api.get('/api/reviews', { params });
      setReviews(res.data.data || res.data || []);
      setTotalPages(res.data.pages || 1);
      setError('');
    } catch (err) {
      setError('Failed to load reviews. Check your backend connection.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { setCurrentPage(1); }, [searchTerm, filterStatus, filterRating, limit]);
  
  useEffect(() => {
    const timer = setTimeout(() => { fetchReviews(); }, 500);
    return () => clearTimeout(timer);
  }, [currentPage, searchTerm, filterStatus, filterRating, limit]);

  const handleDelete = async (id) => {
    if (window.confirm("Are you sure you want to delete this review?")) {
      try { 
        await api.delete(`/api/reviews/${id}`); 
        fetchReviews(); 
      } catch (err) { 
        alert("Failed to delete review."); 
      }
    }
  };

  const handleUpdateStatus = async (id, newStatus) => {
    try {
      await api.patch(`/api/reviews/${id}/status`, { status: newStatus });
      setReviews(reviews.map(r => r.id === id ? { ...r, status: newStatus } : r));
    } catch (err) {
      alert("Failed to update status.");
    }
  };

  const handlePrevPage = () => { if (currentPage > 1) setCurrentPage(prev => prev - 1); };
  const handleNextPage = () => { if (currentPage < totalPages) setCurrentPage(prev => prev + 1); };
  const handlePageClick = (pageNum) => setCurrentPage(pageNum);

  const clearFilters = () => { setSearchTerm(''); setFilterStatus(''); setFilterRating(''); setLimit(10); };

  const renderStars = (rating) => {
    return (
      <div className="flex items-center">
        {[...Array(5)].map((_, i) => (
          <Star key={i} className={`w-4 h-4 ${i < rating ? 'text-yellow-400 fill-yellow-400' : 'text-gray-300'}`} />
        ))}
      </div>
    );
  };

  const getStatusBadge = (status) => {
    switch(status) {
      case 'APPROVED': return <span className="px-2 py-1 text-[0.75rem] font-bold text-green-700 bg-green-100 rounded-full">APPROVED</span>;
      case 'REJECTED': return <span className="px-2 py-1 text-[0.75rem] font-bold text-red-700 bg-red-100 rounded-full">REJECTED</span>;
      case 'PENDING': return <span className="px-2 py-1 text-[0.75rem] font-bold text-yellow-700 bg-yellow-100 rounded-full">PENDING</span>;
      default: return <span className="px-2 py-1 text-[0.75rem] font-bold text-gray-700 bg-gray-100 rounded-full">{status}</span>;
    }
  };

  return (
    <div className="p-6">
      <div className="flex flex-col lg:flex-row lg:items-center justify-between mb-6 gap-4">
        <h1 className="text-xl font-semibold text-gray-800">Review Management</h1>
      </div>

      <div className="bg-white p-4 rounded shadow-sm mb-4 flex flex-col md:flex-row gap-4 items-center">
        <div className="relative flex-1 w-full">
          <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
            <Search className="w-4 h-4 text-gray-400" />
          </div>
          <input 
            type="text" 
            placeholder="Search reviews by user or comment..." 
            value={searchTerm} 
            onChange={(e) => setSearchTerm(e.target.value)} 
            className="block w-full text-[0.85rem] px-3 py-2 pl-9 border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white" 
          />
        </div>
        
        <div className="w-full md:w-48">
          <select 
            value={filterStatus} 
            onChange={(e) => setFilterStatus(e.target.value)} 
            className="w-full text-[0.85rem] px-3 py-2 border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white"
          >
            <option value="">All Statuses</option>
            <option value="APPROVED">Approved</option>
            <option value="PENDING">Pending</option>
            <option value="REJECTED">Rejected</option>
          </select>
        </div>

        <div className="w-full md:w-48">
          <select 
            value={filterRating} 
            onChange={(e) => setFilterRating(e.target.value)} 
            className="w-full text-[0.85rem] px-3 py-2 border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white"
          >
            <option value="">All Ratings</option>
            <option value="5">5 Stars</option>
            <option value="4">4 Stars</option>
            <option value="3">3 Stars</option>
            <option value="2">2 Stars</option>
            <option value="1">1 Star</option>
          </select>
        </div>

        <div className="w-full md:w-32">
          <select 
            value={limit} 
            onChange={(e) => setLimit(Number(e.target.value))} 
            className="w-full text-[0.85rem] px-3 py-2 border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white"
          >
            <option value={10}>10 / page</option>
            <option value={20}>20 / page</option>
            <option value={50}>50 / page</option>
          </select>
        </div>

        {(searchTerm || filterStatus || filterRating || limit !== 10) && (
          <button onClick={clearFilters} className="text-gray-500 hover:text-red-500 flex items-center text-[0.85rem] font-medium">
            <FilterX className="w-4 h-4 mr-1" /> Clear
          </button>
        )}
      </div>

      {error && <p className="mb-4 text-red-500 bg-red-50 p-3 rounded border border-red-200">{error}</p>}

      <div className="border border-gray-100 rounded bg-white shadow-sm flex-1 flex flex-col min-h-0">
        <div className="overflow-x-auto flex-1">
          <table className="w-full text-left border-collapse min-w-[900px]">
            <thead className="bg-gray-50 border-b border-gray-100 sticky top-0 z-10 select-none">
              <tr>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider">User</th>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider">Product Code</th>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider">Rating</th>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider w-1/3">Comment</th>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider">Date</th>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider">Status</th>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-right tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan="7" className="px-4 py-8 text-center text-[0.85rem] text-gray-500">Loading reviews...</td></tr>
              ) : reviews.length === 0 ? (
                <tr><td colSpan="7" className="px-4 py-8 text-center text-[0.85rem] text-gray-500">No reviews found.</td></tr>
              ) : (
                reviews.map((review) => (
                  <tr key={review.id} className="border-b border-gray-50 hover:bg-gray-50/50 transition-colors">
                    <td className="px-4 py-3 text-[0.85rem] text-gray-700">
                      <div className="font-medium text-gray-800">{review.userName || 'Anonymous'}</div>
                      <div className="text-xs text-gray-500 mt-1 truncate max-w-[120px]">{review.userId}</div>
                    </td>
                    <td className="px-4 py-3 text-[0.85rem] text-gray-700">
                      <Link to={`/admin/products/${review.productSlug || review.productId}`} className="font-mono text-xs text-blue-600 bg-blue-50 px-2 py-1 rounded border border-blue-200 hover:bg-blue-100 hover:underline transition-colors inline-block">
                        {review.productCode || review.productId}
                      </Link>
                    </td>
                    <td className="px-4 py-3 text-[0.85rem] text-gray-700">
                      {renderStars(review.rating)}
                    </td>
                    <td className="px-4 py-3 text-[0.85rem] text-gray-700">
                      <p className="text-sm text-gray-700 line-clamp-3">
                        {review.comment || <span className="italic text-gray-400">No comment</span>}
                      </p>
                    </td>
                    <td className="px-4 py-3 text-[0.85rem] text-gray-700 whitespace-nowrap">
                      <div className="flex items-center">
                        <CalendarDays className="w-4 h-4 mr-2 opacity-70" />
                        {new Date(review.createdAt).toLocaleDateString()}
                      </div>
                    </td>
                    <td className="px-4 py-3 text-[0.85rem] text-gray-700">
                      {getStatusBadge(review.status)}
                    </td>
                    <td className="px-4 py-3 text-[0.85rem] text-right whitespace-nowrap space-x-2">
                      {review.status !== 'APPROVED' && (
                        <button 
                          onClick={() => handleUpdateStatus(review.id, 'APPROVED')} 
                          className="inline-flex items-center gap-1.5 bg-white border border-gray-200 text-green-700 hover:bg-green-50 hover:border-green-200 rounded text-[0.80rem] px-2.5 py-1 font-medium shadow-sm transition-colors"
                          title="Approve"
                        >
                          <CheckCircle className="w-3.5 h-3.5" /> Approve
                        </button>
                      )}
                      {review.status !== 'REJECTED' && (
                        <button 
                          onClick={() => handleUpdateStatus(review.id, 'REJECTED')} 
                          className="inline-flex items-center gap-1.5 bg-white border border-gray-200 text-amber-700 hover:bg-amber-50 hover:border-amber-200 rounded text-[0.80rem] px-2.5 py-1 font-medium shadow-sm transition-colors"
                          title="Reject"
                        >
                          <XCircle className="w-3.5 h-3.5" /> Reject
                        </button>
                      )}
                      <button 
                        onClick={() => handleDelete(review.id)} 
                        className="inline-flex items-center gap-1.5 bg-white border border-gray-200 text-gray-600 hover:text-red-700 hover:bg-red-50 hover:border-red-200 rounded text-[0.80rem] px-2.5 py-1 font-medium shadow-sm transition-colors"
                        title="Delete permanently"
                      >
                        <Trash2 className="w-3.5 h-3.5" /> Delete
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        
        {!loading && reviews.length > 0 && (
          <div className="flex items-center justify-between p-4 border-t border-gray-100 bg-gray-50/50">
            <span className="text-[0.85rem] text-gray-600">
              Showing Page <span className="font-semibold">{currentPage}</span> of <span className="font-semibold">{totalPages}</span>
            </span>
            <div className="flex space-x-2 overflow-x-auto max-w-xs md:max-w-md hide-scrollbar">
              <button 
                onClick={handlePrevPage} 
                disabled={currentPage === 1} 
                className={`flex items-center justify-center ${currentPage === 1 ? 'bg-gray-50 border border-gray-200 text-gray-400 cursor-not-allowed' : 'bg-white border border-gray-200 text-gray-700 hover:bg-gray-50'} rounded text-[0.85rem] px-3 py-1.5`}
              >
                Previous
              </button>
              
              {[...Array(totalPages)].map((_, index) => {
                if (
                  totalPages <= 7 ||
                  index === 0 || 
                  index === totalPages - 1 || 
                  (index >= currentPage - 2 && index <= currentPage)
                ) {
                  return (
                    <button 
                      key={index + 1} 
                      onClick={() => handlePageClick(index + 1)} 
                      className={`flex items-center justify-center ${currentPage === index + 1 ? 'bg-blue-600 text-white shadow-sm border border-blue-600' : 'bg-white border border-gray-200 text-gray-700 hover:bg-gray-50'} rounded text-[0.85rem] px-3 py-1.5 min-w-[2rem]`}
                    >
                      {index + 1}
                    </button>
                  );
                }
                if (index === 1 && currentPage > 3) return <span key="ellipsis-1" className="px-2 text-gray-500 self-center">...</span>;
                if (index === totalPages - 2 && currentPage < totalPages - 2) return <span key="ellipsis-2" className="px-2 text-gray-500 self-center">...</span>;
                return null;
              })}

              <button 
                onClick={handleNextPage} 
                disabled={currentPage === totalPages} 
                className={`flex items-center justify-center ${currentPage === totalPages ? 'bg-gray-50 border border-gray-200 text-gray-400 cursor-not-allowed' : 'bg-white border border-gray-200 text-gray-700 hover:bg-gray-50'} rounded text-[0.85rem] px-3 py-1.5`}
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
