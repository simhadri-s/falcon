import { useEffect, useState } from 'react';
import { Search, FilterX, ArrowUpDown, ArrowUp, ArrowDown, X } from 'lucide-react';
import api from '../services/api';
import { compressImageToHdWebp } from '../utils/imageCompression';

export default function AdminNews() {
  const [newsList, setNewsList] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // --- UPDATED: Added Limit State ---
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [limit, setLimit] = useState(10);
  const [searchTerm, setSearchTerm] = useState('');

  const [sortConfig, setSortConfig] = useState({ key: null, direction: 'asc' });

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingNews, setEditingNews] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formData, setFormData] = useState({
    title: '', category: '', content: '', author: '', published: true
  });
  const [imageFile, setImageFile] = useState(null);

  const [showNewCategory, setShowNewCategory] = useState(false);
  const [newCategoryName, setNewCategoryName] = useState('');

  // --- UPDATED: Using Axios params object ---
  const fetchData = async () => {
    setLoading(true);
    try {
      const params = {
        page: currentPage,
        limit: limit,
      };
      
      if (sortConfig.key) {
        params.sortBy = sortConfig.key;
        params.sortDirection = sortConfig.direction;
      }
      
      if (searchTerm) params.search = searchTerm;

      const [newsRes, catRes] = await Promise.all([
        api.get('/api/news', { params }), // Passes parameters correctly
        api.get('/api/categories/news') 
      ]);
      setNewsList(newsRes.data.data || newsRes.data || []);
      setTotalPages(newsRes.data.pages || 1);
      setCategories(catRes.data || []);
    } catch (err) { setError('Failed to load data.'); } finally { setLoading(false); }
  };

  useEffect(() => { setCurrentPage(1); }, [searchTerm, limit]);

  useEffect(() => {
    const timer = setTimeout(() => { fetchData(); }, 500);
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

  const handleCreateCategory = async () => {
    if (!newCategoryName.trim()) return;
    try {
      const res = await api.post('/api/categories/news', { name: newCategoryName.trim() });
      setCategories([...categories, res.data]); setFormData({ ...formData, category: res.data.name }); 
      setNewCategoryName(''); setShowNewCategory(false);
    } catch (err) { alert("Failed to create category."); }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
  };

  const handlePrevPage = () => { if (currentPage > 1) setCurrentPage((prev) => prev - 1); };
  const handleNextPage = () => { if (currentPage < totalPages) setCurrentPage((prev) => prev + 1); };
  const handlePageClick = (pageNum) => setCurrentPage(pageNum);

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData({ ...formData, [name]: type === 'checkbox' ? checked : value });
  };

  const handleAddClick = () => {
    setEditingNews(null);
    setFormData({ title: '', category: '', content: '', author: '', published: true });
    setImageFile(null); setShowNewCategory(false); setIsModalOpen(true);
  };

  const handleEditClick = (newsItem) => {
    setEditingNews(newsItem);
    setFormData({
      title: newsItem.title || '', category: newsItem.category || '', content: newsItem.content || '',
      author: newsItem.author || '', published: newsItem.published !== undefined ? newsItem.published : true,
    });
    setImageFile(null); setShowNewCategory(false); setIsModalOpen(true);
  };

  const handleDeleteClick = async (id) => {
    if (window.confirm("ಖಂಡಿತವಾಗಿಯೂ ಈ ಸುದ್ದಿಯನ್ನು ಅಳಿಸಲು ನೀವು ಬಯಸುವಿರಾ?")) {
      try { await api.delete(`/api/news/${id}`); fetchData(); } catch (err) { alert("Failed to delete."); }
    }
  };

  const handleTogglePublish = async (newsItem) => {
    try {
      const newsId = newsItem.id || newsItem._id;
      const newStatus = !newsItem.published;
      setNewsList(newsList.map(n => (n.id || n._id) === newsId ? { ...n, published: newStatus } : n));
      await api.patch(`/api/news/${newsId}/toggle-publish`);
    } catch (err) { fetchData(); }
  };

  const handleCloseModal = () => { setIsModalOpen(false); setEditingNews(null); };

  const handleSubmit = async (e) => {
    e.preventDefault(); setIsSubmitting(true);
    try {
      const submitData = new FormData();
      const compressedImage = imageFile
        ? await compressImageToHdWebp(imageFile)
        : null;
      submitData.append('title', formData.title); 
      submitData.append('category', formData.category);
      submitData.append('content', formData.content); 
      submitData.append('author', formData.author);
      submitData.append('published', formData.published.toString()); 
      
      // 🌟 ಬದಲಾವಣೆ ಇಲ್ಲಿದೆ: 'image' ಬದಲು 'images' ಬಳಸಿ 
      if (compressedImage) submitData.append('images', compressedImage); 

      const newsId = editingNews?.id || editingNews?._id;
      if (editingNews) await api.put(`/api/news/${newsId}`, submitData);
      else { await api.post('/api/news', submitData); setCurrentPage(1); }
      fetchData(); handleCloseModal();
    } catch (err) { alert("Failed to save."); } finally { setIsSubmitting(false); }
  };

  return (
    <div className="p-6 relative flex flex-col h-full">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-semibold text-gray-800">News & Announcements</h1>
        <button onClick={handleAddClick} className="bg-blue-600 hover:bg-blue-700 text-white rounded text-[0.85rem] font-medium transition-colors shadow-sm px-4 py-2">+ Post News</button>
      </div>

      <div className="bg-white p-4 rounded shadow-sm mb-4 flex flex-col md:flex-row gap-4 items-center">
        <div className="relative flex-1 w-full md:max-w-md">
          <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none"><Search className="w-5 h-5 text-gray-400" /></div>
          <input type="text" placeholder="Search news by title..." value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} className="w-full pl-10 px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white" />
        </div>

        {/* --- NEW: Limit Dropdown --- */}
        <div className="w-full md:w-32">
          <select value={limit} onChange={(e) => setLimit(Number(e.target.value))} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white">
            <option value={10}>10 / page</option>
            <option value={20}>20 / page</option>
            <option value={50}>50 / page</option>
            <option value={100}>100 / page</option>
            <option value={500}>500 / page</option>
            <option value={1000}>1000 / page</option>
          </select>
        </div>

        {(searchTerm || limit !== 10) && <button onClick={() => { setSearchTerm(''); setLimit(10); }} className="text-gray-500 hover:text-red-500 flex items-center text-[0.85rem] font-medium"><FilterX className="w-4 h-4 mr-1" /> Clear</button>}
      </div>

      {error && <p className="mb-4 text-red-500 bg-red-50 p-3 rounded text-[0.85rem]">{error}</p>}

      <div className="bg-white rounded border border-gray-100 shadow-sm overflow-hidden flex flex-col flex-1">
        <div className="overflow-x-auto flex-1">
          <table className="w-full text-left border-collapse min-w-[800px]">
            <thead className="bg-gray-50 border-b border-gray-100 sticky top-0 z-10 select-none">
              <tr>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider w-32">Image</th>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider cursor-pointer hover:bg-gray-100 transition-colors" onClick={() => handleSort('title')}>
                  <div className="flex items-center">Title {getSortIcon('title')}</div>
                </th>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider cursor-pointer hover:bg-gray-100 transition-colors" onClick={() => handleSort('category')}>
                  <div className="flex items-center">Category {getSortIcon('category')}</div>
                </th>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider cursor-pointer hover:bg-gray-100 transition-colors" onClick={() => handleSort('createdAt')}>
                  <div className="flex items-center">Date Posted {getSortIcon('createdAt')}</div>
                </th>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider cursor-pointer hover:bg-gray-100 transition-colors" onClick={() => handleSort('published')}>
                  <div className="flex items-center">Published {getSortIcon('published')}</div>
                </th>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider w-40">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan="6" className="px-4 py-3 text-[0.85rem] text-center text-gray-500">Loading news...</td></tr>
              ) : newsList.length === 0 ? (
                <tr><td colSpan="6" className="px-4 py-3 text-[0.85rem] text-center text-gray-500">No news articles found.</td></tr>
              ) : (
                newsList.map((item) => (
                  <tr key={item.id || item._id} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3 text-[0.85rem] text-gray-700">
                      {item.imageUrl || (item.imageUrls && item.imageUrls[0]) ? (
                        <img src={item.imageUrl || item.imageUrls[0]} alt={item.title} className="w-20 h-16 object-cover rounded shadow-sm border border-gray-200" />
                      ) : <div className="w-20 h-16 bg-gray-100 border border-gray-200 rounded flex items-center justify-center text-xs text-gray-400">No Image</div>}
                    </td>
                    <td className="px-4 py-3 text-[0.85rem] text-gray-700 align-middle max-w-[250px]">
                      <div className="font-medium text-gray-800 line-clamp-2" title={item.title}>{item.title}</div>
                    </td>
                    <td className="px-4 py-3 text-[0.85rem] text-gray-700">
                      <span className="px-2 py-1 text-[0.75rem] font-medium text-blue-600 bg-blue-50 border border-blue-100 rounded-full">{item.category}</span>
                    </td>
                    <td className="px-4 py-3 text-[0.85rem] text-gray-700">{formatDate(item.createdAt)}</td>
                    <td className="px-4 py-3 text-[0.85rem] text-gray-700">
                      <label className="relative inline-flex items-center cursor-pointer">
                        <input type="checkbox" className="sr-only peer" checked={item.published || false} onChange={() => handleTogglePublish(item)} />
                        <div className="w-9 h-5 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-blue-600"></div>
                      </label>
                    </td>
                    <td className="px-4 py-3 text-[0.85rem] text-gray-700">
                      <div className="flex flex-wrap gap-2">
                        <button onClick={() => handleEditClick(item)} className="bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-3 py-1.5 font-medium shadow-sm">Edit</button>
                        <button onClick={() => handleDeleteClick(item.id || item._id)} className="bg-white border border-gray-200 text-red-600 hover:bg-red-50 rounded text-[0.85rem] px-3 py-1.5 font-medium shadow-sm">Delete</button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        {!loading && newsList.length > 0 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-gray-50 mt-auto">
            <span className="text-[0.85rem] text-gray-600">Showing Page <span className="font-semibold">{currentPage}</span> of <span className="font-semibold">{totalPages}</span></span>
            <div className="flex space-x-1 overflow-x-auto max-w-xs md:max-w-md hide-scrollbar">
              <button onClick={handlePrevPage} disabled={currentPage === 1} className={`bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-3 py-1.5 font-medium shadow-sm ${currentPage === 1 ? 'opacity-50 cursor-not-allowed' : ''}`}>Prev</button>
              {[...Array(totalPages)].map((_, index) => (
                <button key={index + 1} onClick={() => handlePageClick(index + 1)} className={`rounded text-[0.85rem] px-3 py-1.5 font-medium shadow-sm border ${currentPage === index + 1 ? 'bg-blue-50 text-blue-600 border-blue-200' : 'bg-white border-gray-200 text-gray-700 hover:bg-gray-50'}`}>{index + 1}</button>
              ))}
              <button onClick={handleNextPage} disabled={currentPage === totalPages} className={`bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-3 py-1.5 font-medium shadow-sm ${currentPage === totalPages ? 'opacity-50 cursor-not-allowed' : ''}`}>Next</button>
            </div>
          </div>
        )}
      </div>

      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="w-full max-w-2xl bg-white rounded shadow-xl overflow-hidden flex flex-col max-h-[90vh]">
            <div className="p-6 border-b border-gray-100 flex justify-between items-center">
              <h2 className="text-xl font-semibold text-gray-800">{editingNews ? 'Edit News' : 'Post New Article'}</h2>
              <button onClick={handleCloseModal} className="text-gray-400 hover:text-red-500"><X className="w-5 h-5" /></button>
            </div>
            
            <form onSubmit={handleSubmit} className="p-6 overflow-y-auto flex-1">
              <div className="grid grid-cols-2 gap-6 mb-6">
                <div className="col-span-2">
                  <label className="block mb-1 font-medium text-[0.85rem] text-gray-700">Article Title <span className="text-red-500">*</span></label>
                  <input type="text" name="title" required value={formData.title} onChange={handleInputChange} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white" />
                </div>
                
                <div className="col-span-2 md:col-span-1">
                  <label className="block mb-1 font-medium text-[0.85rem] text-gray-700">Category <span className="text-red-500">*</span></label>
                  {!showNewCategory ? (
                    <div className="flex gap-2">
                      <select name="category" required value={formData.category} onChange={handleInputChange} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white">
                        <option value="" disabled>Select Category</option>
                        {categories.map(cat => (
                          <option key={cat.id || cat._id} value={cat.name}>{cat.name}</option>
                        ))}
                      </select>
                      <button type="button" onClick={() => setShowNewCategory(true)} className="bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-3 py-1.5 font-medium shadow-sm flex items-center justify-center">+</button>
                    </div>
                  ) : (
                    <div className="flex gap-2">
                      <input type="text" placeholder="New category name" value={newCategoryName} onChange={(e) => setNewCategoryName(e.target.value)} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white" autoFocus />
                      <button type="button" onClick={handleCreateCategory} className="bg-blue-600 hover:bg-blue-700 text-white rounded text-[0.85rem] font-medium transition-colors shadow-sm px-4 py-2">Save</button>
                      <button type="button" onClick={() => setShowNewCategory(false)} className="bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-3 py-1.5 font-medium shadow-sm flex items-center justify-center"><X className="w-4 h-4" /></button>
                    </div>
                  )}
                </div>

                <div className="col-span-2 md:col-span-1">
                  <label className="block mb-1 font-medium text-[0.85rem] text-gray-700">Author</label>
                  <input type="text" name="author" value={formData.author} onChange={handleInputChange} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white" placeholder="e.g. Admin / John Doe" />
                </div>

                <div className="col-span-2">
                  <label className="block mb-1 font-medium text-[0.85rem] text-gray-700">Full Content <span className="text-red-500">*</span></label>
                  <textarea name="content" required value={formData.content} onChange={handleInputChange} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white h-40"></textarea>
                </div>
                
                <div className="col-span-2 md:col-span-1 flex items-center mt-2">
                  <input type="checkbox" id="published" name="published" checked={formData.published} onChange={handleInputChange} className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500" />
                  <label htmlFor="published" className="ml-2 font-medium text-[0.85rem] text-gray-700 cursor-pointer">Publish immediately</label>
                </div>

                <div className="col-span-2 p-4 border-2 border-dashed border-gray-200 rounded bg-gray-50">
                  <label className="block mb-2 font-medium text-[0.85rem] text-gray-700">Cover Image</label>
                  <input type="file" accept="image/*" onChange={(e) => setImageFile(e.target.files[0])} className="w-full text-[0.85rem] text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded file:border-0 file:text-[0.85rem] file:font-medium file:bg-white file:text-gray-700 file:border file:border-gray-200 hover:file:bg-gray-50" />
                </div>
              </div>

              <div className="flex justify-end pt-4 space-x-3 border-t border-gray-100 mt-4">
                <button type="button" onClick={handleCloseModal} className="bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-3 py-1.5 font-medium shadow-sm">Cancel</button>
                <button type="submit" disabled={isSubmitting} className="bg-blue-600 hover:bg-blue-700 text-white rounded text-[0.85rem] font-medium transition-colors shadow-sm px-4 py-2 disabled:opacity-50">{isSubmitting ? 'Saving...' : (editingNews ? 'Update Article' : 'Post Article')}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
