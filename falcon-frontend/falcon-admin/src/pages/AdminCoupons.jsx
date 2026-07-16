import { useEffect, useState } from 'react';
import { Search, FilterX, Tag, Plus, Trash2, ToggleLeft, ToggleRight, X } from 'lucide-react';
import api from '../services/api';

export default function AdminCoupons() {
  const [coupons, setCoupons] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [limit] = useState(10);
  const [searchTerm, setSearchTerm] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [formData, setFormData] = useState({
    code: '',
    discountType: 'FLAT',
    discountValue: '',
    minOrderValue: '',
    expiryDate: '',
    maxUses: '',
    isActive: true,
    applicableProductIds: [],
    applicableCategoryIds: [],
  });

  const [categories, setCategories] = useState([]);
  const [products, setProducts] = useState([]);
  const [loadingExtras, setLoadingExtras] = useState(false);
  const [catSearch, setCatSearch] = useState('');
  const [prodSearch, setProdSearch] = useState('');

  const fetchCoupons = async () => {
    setLoading(true);
    try {
      const params = { page: currentPage, limit };
      if (searchTerm) params.keyword = searchTerm;
      const res = await api.get('/api/coupons', { params });
      setCoupons(res.data.data || []);
      setTotalPages(res.data.pages || 1);
    } catch (err) {
      setError('Failed to load coupons.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { setCurrentPage(1); }, [searchTerm]);
  useEffect(() => {
    const timer = setTimeout(fetchCoupons, 400);
    return () => clearTimeout(timer);
  }, [currentPage, searchTerm]);

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
  };

  const handleOpenModal = async () => {
    setFormData({ 
      code: '', 
      discountType: 'FLAT', 
      discountValue: '', 
      minOrderValue: '', 
      expiryDate: '', 
      maxUses: '', 
      isActive: true,
      applicableProductIds: [],
      applicableCategoryIds: [],
    });
    setCatSearch('');
    setProdSearch('');
    setIsModalOpen(true);
    
    // Fetch products and categories for the dropdowns
    setLoadingExtras(true);
    try {
      const [catRes, prodRes] = await Promise.all([
        api.get('/api/categories/products'),
        api.get('/api/products?limit=1000') // Fetch a good number of products
      ]);
      setCategories(catRes.data || []);
      setProducts(prodRes.data.data || []);
    } catch (err) {
      console.error('Failed to load restriction options:', err);
    } finally {
      setLoadingExtras(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.code.trim()) { alert('Coupon code is required.'); return; }
    if (!formData.discountValue || Number(formData.discountValue) <= 0) { alert('Discount value must be greater than 0.'); return; }
    if (formData.discountType === 'PERCENTAGE' && Number(formData.discountValue) > 100) { alert('Percentage discount cannot exceed 100.'); return; }

    setIsSubmitting(true);
    try {
      const payload = {
        code: formData.code.trim().toUpperCase(),
        discountType: formData.discountType,
        discountValue: Number(formData.discountValue),
        minOrderValue: formData.minOrderValue ? Number(formData.minOrderValue) : 0,
        expiryDate: formData.expiryDate ? formData.expiryDate : null,
        maxUses: formData.maxUses ? Number(formData.maxUses) : 0,
        isActive: formData.isActive,
        applicableProductIds: formData.applicableProductIds,
        applicableCategoryIds: formData.applicableCategoryIds,
      };
      await api.post('/api/coupons', payload);
      setIsModalOpen(false);
      fetchCoupons();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to create coupon.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleToggle = async (coupon) => {
    try {
      const updated = await api.patch(`/api/coupons/${coupon.id}/toggle`);
      setCoupons(prev => prev.map(c => c.id === coupon.id ? updated.data : c));
    } catch {
      alert('Failed to toggle coupon status.');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this coupon? This cannot be undone.')) return;
    try {
      await api.delete(`/api/coupons/${id}`);
      fetchCoupons();
    } catch {
      alert('Failed to delete coupon.');
    }
  };

  const formatDate = (iso) => {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  };

  const isExpired = (expiryDate) => expiryDate && new Date(expiryDate) < new Date();

  return (
    <div className="relative flex flex-col h-full p-6">
      {/* Header */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between mb-6 gap-4">
        <h1 className="text-xl font-semibold text-gray-800 flex items-center gap-2">
          <Tag className="w-5 h-5 text-gray-500" /> Coupon Management
        </h1>
        <button
          onClick={handleOpenModal}
          className="bg-blue-600 hover:bg-blue-700 text-white rounded text-[0.85rem] font-medium transition-colors shadow-sm px-4 py-2 flex items-center"
        >
          <Plus className="w-4 h-4 mr-2" /> Create Coupon
        </button>
      </div>

      {/* Search */}
      <div className="bg-white p-4 rounded border border-gray-100 shadow-sm mb-4 flex gap-4 items-center">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            type="text"
            placeholder="Search coupon codes..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white"
          />
        </div>
        {searchTerm && (
          <button onClick={() => setSearchTerm('')} className="bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-3 py-1.5 font-medium shadow-sm flex items-center">
            <FilterX className="w-4 h-4 mr-1" /> Clear
          </button>
        )}
      </div>

      {error && <p className="mb-4 text-[0.85rem] text-red-600 bg-red-50 border border-red-100 px-4 py-3 rounded">{error}</p>}

      {/* Table */}
      <div className="bg-white rounded border border-gray-100 shadow-sm overflow-hidden flex flex-col flex-1">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse min-w-[900px]">
            <thead className="bg-gray-50 border-b border-gray-100 sticky top-0 z-10">
              <tr>
                {['Code', 'Type', 'Discount', 'Min Order', 'Expiry', 'Max Uses', 'Used', 'Status', 'Actions'].map(th => (
                  <th key={th} className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider">
                    {th}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan="9" className="px-4 py-8 text-center text-[0.85rem] text-gray-500">Loading coupons...</td></tr>
              ) : coupons.length === 0 ? (
                <tr><td colSpan="9" className="px-4 py-8 text-center text-[0.85rem] text-gray-500">No coupons found.</td></tr>
              ) : (
                coupons.map((coupon) => {
                  const expired = isExpired(coupon.expiryDate);
                  return (
                    <tr key={coupon.id} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                      <td className="px-4 py-3 text-[0.85rem] text-gray-700">
                        <span className="font-mono font-medium text-gray-800 bg-gray-100 px-2 py-1 rounded border border-gray-200 text-[0.85rem]">
                          {coupon.code}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-[0.85rem] text-gray-700">
                        <span className={`text-[0.75rem] font-medium px-2 py-1 rounded ${coupon.discountType === 'PERCENTAGE' ? 'bg-purple-50 text-purple-700 border border-purple-100' : 'bg-blue-50 text-blue-700 border border-blue-100'}`}>
                          {coupon.discountType}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-[0.85rem] text-gray-700">
                        <span className="font-medium text-gray-800">
                          {coupon.discountType === 'PERCENTAGE' ? `${coupon.discountValue}%` : `₹${coupon.discountValue.toLocaleString('en-IN')}`}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-[0.85rem] text-gray-700">
                        {coupon.minOrderValue > 0 ? `₹${coupon.minOrderValue.toLocaleString('en-IN')}` : '—'}
                      </td>
                      <td className="px-4 py-3 text-[0.85rem] text-gray-700">
                        <span className={expired ? 'text-red-600 font-medium' : 'text-gray-700'}>
                          {formatDate(coupon.expiryDate)}
                          {expired && <span className="ml-1 text-[0.75rem] text-red-500">(Expired)</span>}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-[0.85rem] text-gray-700">
                        {coupon.maxUses === 1 ? (
                          <span className="text-[0.75rem] font-medium text-amber-700 bg-amber-50 border border-amber-100 px-2 py-0.5 rounded">Single-use</span>
                        ) : coupon.maxUses === 0 ? (
                          <span className="text-[0.75rem] text-gray-500">Unlimited</span>
                        ) : (
                          <span>{coupon.maxUses}</span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-[0.85rem] text-gray-700">
                        <span className="font-medium text-gray-800">{coupon.usedCount}</span>
                        {coupon.maxUses > 0 && (
                          <span className="text-gray-400 text-[0.75rem]"> / {coupon.maxUses}</span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-[0.85rem] text-gray-700">
                        <button
                          onClick={() => handleToggle(coupon)}
                          className={`flex items-center gap-1.5 text-[0.75rem] font-medium px-2 py-1 rounded transition-colors ${coupon.active ? 'bg-green-50 text-green-700 hover:bg-green-100 border border-green-100' : 'bg-gray-50 text-gray-500 hover:bg-gray-100 border border-gray-200'}`}
                        >
                          {coupon.active ? <ToggleRight className="w-4 h-4" /> : <ToggleLeft className="w-4 h-4" />}
                          {coupon.active ? 'Active' : 'Inactive'}
                        </button>
                      </td>
                      <td className="px-4 py-3 text-[0.85rem] text-gray-700">
                        <button
                          onClick={() => handleDelete(coupon.id)}
                          className="flex items-center gap-1 px-3 py-1.5 text-[0.75rem] font-medium text-red-600 bg-red-50 rounded hover:bg-red-100 border border-red-100 transition-colors shadow-sm"
                        >
                          <Trash2 className="w-3.5 h-3.5" /> Delete
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {!loading && coupons.length > 0 && (
          <div className="flex items-center justify-between p-4 border-t border-gray-100 bg-white">
            <span className="text-[0.85rem] text-gray-600">
              Page <span className="font-semibold text-gray-800">{currentPage}</span> of <span className="font-semibold text-gray-800">{totalPages}</span>
            </span>
            <div className="flex space-x-2">
              <button
                onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                disabled={currentPage === 1}
                className={`bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-3 py-1.5 font-medium shadow-sm ${currentPage === 1 ? 'opacity-50 cursor-not-allowed' : ''}`}
              >
                Previous
              </button>
              {[...Array(totalPages)].map((_, i) => (
                <button
                  key={i}
                  onClick={() => setCurrentPage(i + 1)}
                  className={`rounded text-[0.85rem] px-3 py-1.5 font-medium shadow-sm border ${currentPage === i + 1 ? 'bg-blue-50 border-blue-200 text-blue-700' : 'bg-white border-gray-200 text-gray-700 hover:bg-gray-50'}`}
                >
                  {i + 1}
                </button>
              ))}
              <button
                onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
                disabled={currentPage === totalPages}
                className={`bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-3 py-1.5 font-medium shadow-sm ${currentPage === totalPages ? 'opacity-50 cursor-not-allowed' : ''}`}
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Create Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="w-full max-w-lg bg-white rounded shadow-xl flex flex-col max-h-[95vh]">
            <div className="p-5 border-b border-gray-100 flex justify-between items-center bg-white rounded-t">
              <h2 className="text-xl font-semibold text-gray-800 flex items-center gap-2">
                <Tag className="w-5 h-5 text-gray-500" /> Create New Coupon
              </h2>
              <button onClick={() => setIsModalOpen(false)} className="text-gray-400 hover:text-gray-600 transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>
            <form onSubmit={handleSubmit} className="p-5 overflow-y-auto flex-1 space-y-4">
              {/* Code */}
              <div>
                <label className="block mb-1 text-[0.85rem] font-medium text-gray-700">Coupon Code <span className="text-red-500">*</span></label>
                <input
                  type="text"
                  name="code"
                  required
                  value={formData.code}
                  onChange={handleInputChange}
                  placeholder="e.g. SAVE200"
                  className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white uppercase"
                  style={{ textTransform: 'uppercase' }}
                />
                <p className="text-[0.75rem] text-gray-400 mt-1">Will be stored in uppercase automatically.</p>
              </div>

              {/* Discount Type + Value */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block mb-1 text-[0.85rem] font-medium text-gray-700">Discount Type <span className="text-red-500">*</span></label>
                  <select
                    name="discountType"
                    value={formData.discountType}
                    onChange={handleInputChange}
                    className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white"
                  >
                    <option value="FLAT">Flat (₹)</option>
                    <option value="PERCENTAGE">Percentage (%)</option>
                  </select>
                </div>
                <div>
                  <label className="block mb-1 text-[0.85rem] font-medium text-gray-700">
                    Discount Value <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="number"
                    name="discountValue"
                    min="0.01"
                    step="0.01"
                    required
                    value={formData.discountValue}
                    onChange={handleInputChange}
                    placeholder={formData.discountType === 'PERCENTAGE' ? 'e.g. 10' : 'e.g. 200'}
                    className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white"
                    onWheel={(e) => e.target.blur()}
                  />
                </div>
              </div>

              {/* Min Order Value */}
              <div>
                <label className="block mb-1 text-[0.85rem] font-medium text-gray-700">Minimum Order Value (₹)</label>
                <input
                  type="number"
                  name="minOrderValue"
                  min="0"
                  value={formData.minOrderValue}
                  onChange={handleInputChange}
                  placeholder="0 = no minimum"
                  className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white"
                  onWheel={(e) => e.target.blur()}
                />
              </div>

              {/* Expiry Date */}
              <div>
                <label className="block mb-1 text-[0.85rem] font-medium text-gray-700">Expiry Date (optional)</label>
                <input
                  type="datetime-local"
                  name="expiryDate"
                  value={formData.expiryDate}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white"
                />
              </div>

              {/* Max Uses */}
              <div>
                <label className="block mb-1 text-[0.85rem] font-medium text-gray-700">Max Uses</label>
                <input
                  type="number"
                  name="maxUses"
                  min="0"
                  step="1"
                  value={formData.maxUses}
                  onChange={handleInputChange}
                  placeholder="0 = unlimited, 1 = single-use"
                  className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white"
                  onWheel={(e) => e.target.blur()}
                />
                <div className="mt-2 grid grid-cols-2 gap-2">
                  <button type="button" onClick={() => setFormData(p => ({...p, maxUses: '1'}))}
                    className={`text-[0.75rem] py-1.5 px-2 rounded border font-medium transition-colors ${formData.maxUses === '1' ? 'bg-blue-50 text-blue-700 border-blue-200' : 'bg-gray-50 text-gray-600 border-gray-200 hover:bg-gray-100'}`}>
                    Single-use (1 person)
                  </button>
                  <button type="button" onClick={() => setFormData(p => ({...p, maxUses: '0'}))}
                    className={`text-[0.75rem] py-1.5 px-2 rounded border font-medium transition-colors ${formData.maxUses === '0' ? 'bg-blue-50 text-blue-700 border-blue-200' : 'bg-gray-50 text-gray-600 border-gray-200 hover:bg-gray-100'}`}>
                    Unlimited (each user once)
                  </button>
                </div>
                <p className="text-[0.75rem] text-gray-500 mt-2">
                  <strong>Single-use:</strong> Only 1 person can ever claim it. &nbsp;
                  <strong>Unlimited:</strong> Anyone can use it, but each user only once.
                </p>
              </div>

              {/* Restrictions */}
              <div className="space-y-4 pt-4 border-t border-gray-100">
                <h3 className="font-semibold text-gray-800 text-[0.85rem] flex items-center gap-2">
                  <FilterX className="w-4 h-4 text-gray-500" /> Restrictions (Optional)
                </h3>
                
                <div>
                  <label className="block mb-1 text-[0.85rem] font-medium text-gray-700">Apply to Specific Categories</label>
                  <div className="mb-2 relative">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-400" />
                    <input 
                      type="text" 
                      placeholder="Search categories..."
                      value={catSearch}
                      onChange={(e) => setCatSearch(e.target.value)}
                      className="w-full pl-9 px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white"
                    />
                  </div>
                  {loadingExtras ? <p className="text-[0.75rem] text-gray-400">Loading categories...</p> : (
                    <div className="grid grid-cols-2 gap-2 max-h-32 overflow-y-auto p-2 border border-gray-200 rounded bg-gray-50">
                      {categories.filter(c => c.name.toLowerCase().includes(catSearch.toLowerCase())).map(cat => (
                        <label key={cat.id} className="flex items-center gap-2 text-[0.85rem] cursor-pointer hover:bg-gray-100 p-1.5 rounded text-gray-700">
                          <input 
                            type="checkbox"
                            checked={formData.applicableCategoryIds.includes(cat.id)}
                            onChange={(e) => {
                              const ids = e.target.checked 
                                ? [...formData.applicableCategoryIds, cat.id]
                                : formData.applicableCategoryIds.filter(id => id !== cat.id);
                              setFormData(p => ({ ...p, applicableCategoryIds: ids }));
                            }}
                            className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                          />
                          <span className="truncate">{cat.name}</span>
                        </label>
                      ))}
                    </div>
                  )}
                  <p className="text-[0.75rem] text-gray-500 mt-1">Leave empty to apply to all categories.</p>
                </div>

                <div>
                  <label className="block mb-1 text-[0.85rem] font-medium text-gray-700">Apply to Specific Products</label>
                  <div className="mb-2 relative">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-400" />
                    <input 
                      type="text" 
                      placeholder="Search products..."
                      value={prodSearch}
                      onChange={(e) => setProdSearch(e.target.value)}
                      className="w-full pl-9 px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white"
                    />
                  </div>
                  {loadingExtras ? <p className="text-[0.75rem] text-gray-400">Loading products...</p> : (
                    <div className="space-y-1 max-h-40 overflow-y-auto p-2 border border-gray-200 rounded bg-gray-50">
                      {products.filter(p => p.name.toLowerCase().includes(prodSearch.toLowerCase()) || p.productCode?.toLowerCase().includes(prodSearch.toLowerCase())).map(prod => (
                        <label key={prod.id} className="flex items-center gap-2 text-[0.85rem] cursor-pointer hover:bg-gray-100 p-1.5 rounded text-gray-700">
                          <input 
                            type="checkbox"
                            checked={formData.applicableProductIds.includes(prod.id)}
                            onChange={(e) => {
                              const ids = e.target.checked 
                                ? [...formData.applicableProductIds, prod.id]
                                : formData.applicableProductIds.filter(id => id !== prod.id);
                              setFormData(p => ({ ...p, applicableProductIds: ids }));
                            }}
                            className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                          />
                          <span className="truncate flex-1">{prod.name}</span>
                          <span className="text-[0.75rem] text-gray-400">{prod.productCode}</span>
                        </label>
                      ))}
                    </div>
                  )}
                  <p className="text-[0.75rem] text-gray-500 mt-1">Leave empty to apply to all products.</p>
                </div>
              </div>

              {/* Active Toggle */}
              <div className="flex items-center gap-3 pt-4 border-t border-gray-100">
                <input
                  type="checkbox"
                  id="isActive"
                  name="isActive"
                  checked={formData.isActive}
                  onChange={handleInputChange}
                  className="w-4 h-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                />
                <label htmlFor="isActive" className="text-[0.85rem] font-medium text-gray-700 cursor-pointer">
                  Active immediately
                </label>
              </div>

              <div className="flex justify-end space-x-3 pt-5 mt-2 border-t border-gray-100">
                <button type="button" onClick={() => setIsModalOpen(false)}
                  className="bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-4 py-2 font-medium shadow-sm transition-colors">
                  Cancel
                </button>
                <button type="submit" disabled={isSubmitting}
                  className="bg-blue-600 hover:bg-blue-700 text-white rounded text-[0.85rem] font-medium transition-colors shadow-sm px-4 py-2 disabled:opacity-50">
                  {isSubmitting ? 'Creating...' : 'Create Coupon'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
