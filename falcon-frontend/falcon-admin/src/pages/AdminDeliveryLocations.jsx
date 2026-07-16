import { useEffect, useState } from 'react';
import { Search, FilterX, ArrowUpDown, ArrowUp, ArrowDown, MapPin, X } from 'lucide-react';
import api from '../services/api';

export default function AdminDeliveryLocations() {
  const [locations, setLocations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Pagination & Search States
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [limit, setLimit] = useState(10);
  const [searchTerm, setSearchTerm] = useState('');

  const [sortConfig, setSortConfig] = useState({ key: null, direction: 'asc' });

  // Modal & Form State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingLocation, setEditingLocation] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  const [formData, setFormData] = useState({
    pincode: '', location: '', deliveryCharge: ''
  });

  const fetchLocations = async () => {
    setLoading(true);
    try {
      const params = { page: currentPage, limit: limit };
      if (searchTerm) params.search = searchTerm;
      if (sortConfig.key) {
        params.sortBy = sortConfig.key;
        params.sortDirection = sortConfig.direction;
      }

      const response = await api.get('/api/delivery-location', { params });
      setLocations(response.data.data || []);
      setTotalPages(response.data.pages || 1);
    } catch (err) {
      setError('Failed to load delivery locations.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { setCurrentPage(1); }, [searchTerm, limit]);

  useEffect(() => {
    const timer = setTimeout(() => { fetchLocations(); }, 500);
    return () => clearTimeout(timer);
  }, [currentPage, searchTerm, limit, sortConfig]);

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

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const handleAddClick = () => {
    setEditingLocation(null);
    setFormData({ pincode: '', location: '', deliveryCharge: '' });
    setIsModalOpen(true);
  };

  const handleEditClick = (loc) => {
    setEditingLocation(loc);
    setFormData({
      pincode: loc.pincode || '',
      location: loc.location || '',
      deliveryCharge: loc.deliveryCharge !== undefined ? loc.deliveryCharge : ''
    });
    setIsModalOpen(true);
  };

  const handleDeleteClick = async (id) => {
    if (window.confirm("ಖಂಡಿತವಾಗಿಯೂ ಈ ಸ್ಥಳವನ್ನು ಅಳಿಸಲು ನೀವು ಬಯಸುವಿರಾ? (Are you sure you want to delete this location?)")) {
      try {
        await api.delete(`/api/delivery-location/${id}`);
        fetchLocations(); 
      } catch (err) { alert("Failed to delete location."); }
    }
  };

  const handleCloseModal = () => { setIsModalOpen(false); setEditingLocation(null); };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Validating pincode locally before sending to backend
    if (!/^[1-9][0-9]{5}$/.test(formData.pincode)) {
      alert("Invalid Pincode. It must be exactly 6 digits and cannot start with 0.");
      return;
    }

    setIsSubmitting(true);
    try {
      // Create JSON payload
      const payload = {
        pincode: formData.pincode,
        location: formData.location,
        deliveryCharge: parseFloat(formData.deliveryCharge) || 0
      };

      const locId = editingLocation?.id || editingLocation?._id;
      
      if (editingLocation) {
        await api.put(`/api/delivery-location/${locId}`, payload);
      } else {
        await api.post('/api/delivery-location', payload);
        setCurrentPage(1);
      }
      
      fetchLocations();
      handleCloseModal();
    } catch (err) {
      console.error("Save Error:", err.response || err);
      const errorMsg = err.response?.data?.message || "";
      
      // Handle Duplicate pincode Error from MongoDB
      if (errorMsg.includes('duplicate key') || errorMsg.includes('pincode')) {
        alert("⚠️ Error: This Pincode already exists! Please enter a unique Pincode.");
      } else {
        alert(errorMsg || "Failed to save location. Please check your inputs.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const clearFilters = () => { setSearchTerm(''); setLimit(10); };

  return (
    <div className="relative flex flex-col h-full pb-6">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-semibold text-gray-800">Delivery Locations</h1>
        <button onClick={handleAddClick} className="bg-blue-600 hover:bg-blue-700 text-white rounded text-[0.85rem] font-medium transition-colors shadow-sm px-4 py-2">
          + Add New Location
        </button>
      </div>

      <div className="bg-white p-4 rounded border border-gray-100 shadow-sm mb-4 flex flex-col md:flex-row gap-4 items-center">
        <div className="relative flex-1 w-full md:max-w-md">
          <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
            <Search className="w-4 h-4 text-gray-400" />
          </div>
          <input 
            type="text" 
            placeholder="Search by location name or pin code..." 
            value={searchTerm} 
            onChange={(e) => setSearchTerm(e.target.value)} 
            className="w-full pl-10 pr-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white" 
          />
        </div>

        <div className="w-full md:w-32">
          <select value={limit} onChange={(e) => setLimit(Number(e.target.value))} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white">
            <option value={10}>10 / page</option>
            <option value={20}>20 / page</option>
            <option value={50}>50 / page</option>
            <option value={100}>100 / page</option>
          </select>
        </div>

        {(searchTerm || limit !== 10) && (
          <button onClick={clearFilters} className="text-gray-500 hover:text-red-500 flex items-center text-[0.85rem] font-medium">
            <FilterX className="w-4 h-4 mr-1" /> Clear
          </button>
        )}
      </div>

      {error && <p className="mb-4 text-red-500 bg-red-50 p-3 rounded border border-red-100 text-[0.85rem]">{error}</p>}

      <div className="bg-white rounded border border-gray-100 shadow-sm overflow-hidden flex flex-col flex-1">
        <div className="overflow-x-auto overflow-y-auto flex-1">
          <table className="w-full text-left border-collapse">
            <thead className="bg-gray-50 border-b border-gray-100 sticky top-0 z-10 select-none">
              <tr>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider w-16">Icon</th>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider cursor-pointer hover:bg-gray-100 transition-colors" onClick={() => handleSort('pincode')}>
                  <div className="flex items-center">Pincode {getSortIcon('pincode')}</div>
                </th>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider cursor-pointer hover:bg-gray-100 transition-colors" onClick={() => handleSort('location')}>
                  <div className="flex items-center">Location Name {getSortIcon('location')}</div>
                </th>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider cursor-pointer hover:bg-gray-100 transition-colors" onClick={() => handleSort('deliveryCharge')}>
                  <div className="flex items-center">Delivery Charge (₹) {getSortIcon('deliveryCharge')}</div>
                </th>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider w-40">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan="5" className="px-4 py-8 text-center text-gray-500 text-[0.85rem]">Loading locations...</td></tr>
              ) : locations.length === 0 ? (
                <tr><td colSpan="5" className="px-4 py-8 text-center text-gray-500 text-[0.85rem]">No delivery locations found.</td></tr>
              ) : (
                locations.map((loc) => (
                  <tr key={loc.id || loc._id} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3 text-[0.85rem] text-gray-400">
                      <MapPin className="w-4 h-4" />
                    </td>
                    <td className="px-4 py-3 text-[0.85rem] text-gray-700 font-mono tracking-wider">{loc.pincode}</td>
                    <td className="px-4 py-3 text-[0.85rem] text-gray-700">{loc.location}</td>
                    <td className="px-4 py-3 text-[0.85rem] text-gray-700">₹ {Number(loc.deliveryCharge).toFixed(2)}</td>
                    <td className="px-4 py-3 text-[0.85rem] text-gray-700">
                      <div className="flex flex-wrap gap-2">
                        <button onClick={() => handleEditClick(loc)} className="bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-3 py-1.5 font-medium shadow-sm">Edit</button>
                        <button onClick={() => handleDeleteClick(loc.id || loc._id)} className="bg-white border border-gray-200 text-red-600 hover:bg-red-50 rounded text-[0.85rem] px-3 py-1.5 font-medium shadow-sm">Delete</button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {!loading && locations.length > 0 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-gray-50 mt-auto">
            <span className="text-[0.85rem] text-gray-600">Showing Page <span className="font-medium text-gray-800">{currentPage}</span> of <span className="font-medium text-gray-800">{totalPages}</span></span>
            <div className="flex space-x-2">
              <button onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))} disabled={currentPage === 1} className={`bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-3 py-1.5 font-medium shadow-sm ${currentPage === 1 ? 'opacity-50 cursor-not-allowed' : ''}`}>Prev</button>
              <button onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))} disabled={currentPage === totalPages} className={`bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-3 py-1.5 font-medium shadow-sm ${currentPage === totalPages ? 'opacity-50 cursor-not-allowed' : ''}`}>Next</button>
            </div>
          </div>
        )}
      </div>

      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="w-full max-w-md bg-white rounded shadow-xl flex flex-col">
            <div className="p-5 border-b border-gray-100 flex justify-between items-center">
              <h2 className="text-xl font-semibold text-gray-800">{editingLocation ? 'Edit Location' : 'Add New Location'}</h2>
              <button onClick={handleCloseModal} className="text-gray-400 hover:text-gray-600 transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>
            
            <form onSubmit={handleSubmit} className="p-5">
              <div className="mb-4">
                <label className="block mb-1 text-[0.85rem] font-medium text-gray-700">Pincode <span className="text-red-500">*</span></label>
                <input 
                  type="text" 
                  name="pincode" 
                  required 
                  maxLength={6}
                  value={formData.pincode} 
                  onChange={handleInputChange} 
                  className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white font-mono tracking-widest" 
                  placeholder="e.g. 560001" 
                />
              </div>

              <div className="mb-4">
                <label className="block mb-1 text-[0.85rem] font-medium text-gray-700">Location Name <span className="text-red-500">*</span></label>
                <input 
                  type="text" 
                  name="location" 
                  required 
                  value={formData.location} 
                  onChange={handleInputChange} 
                  className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white" 
                  placeholder="e.g. MG Road, Bengaluru" 
                />
              </div>

              <div className="mb-6">
                <label className="block mb-1 text-[0.85rem] font-medium text-gray-700">Delivery Charge (₹) <span className="text-red-500">*</span></label>
                <input 
                  type="number" 
                  name="deliveryCharge" 
                  required 
                  min="0"
                  step="0.01"
                  value={formData.deliveryCharge} 
                  onChange={handleInputChange} 
                  className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white" 
                  placeholder="e.g. 50.00" 
                />
              </div>
              
              <div className="flex justify-end pt-4 gap-3">
                <button type="button" onClick={handleCloseModal} className="bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-3 py-1.5 font-medium shadow-sm">Cancel</button>
                <button type="submit" disabled={isSubmitting} className="bg-blue-600 hover:bg-blue-700 text-white rounded text-[0.85rem] font-medium transition-colors shadow-sm px-4 py-2 disabled:opacity-50">
                  {isSubmitting ? 'Saving...' : 'Save Location'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}