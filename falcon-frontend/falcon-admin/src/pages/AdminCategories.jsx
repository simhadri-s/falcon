import React, { useEffect, useState } from 'react';
import { Search, Plus, Edit, Trash2, Loader2, Tag, ChevronDown, ChevronRight, X } from 'lucide-react';
import api from '../services/api';
import { compressImageToHdWebp } from '../utils/imageCompression';

export default function AdminCategories() {
  const [categories, setCategories] = useState([]);
  const [subCategories, setSubCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [expandedCategories, setExpandedCategories] = useState(new Set());

  // Modal & Form States
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [modalType, setModalType] = useState('category'); // 'category' or 'subcategory'
  const [editingId, setEditingId] = useState(null);

  const initialFormState = { name: '', slug: '', categoryId: '', image: null };
  const [formData, setFormData] = useState(initialFormState);
  const [imagePreview, setImagePreview] = useState(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [catRes, subRes] = await Promise.all([
        api.get('/api/categories/products'),
        api.get('/api/categories/sub')
      ]);
      setCategories(catRes.data || []);
      setSubCategories(subRes.data || []);
    } catch (err) {
      setError("Failed to load categories.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const toggleCategory = (catId) => {
    const newExpanded = new Set(expandedCategories);
    if (newExpanded.has(catId)) {
      newExpanded.delete(catId);
    } else {
      newExpanded.add(catId);
    }
    setExpandedCategories(newExpanded);
  };

  const handleInputChange = (e) => {
    const { name, value, files } = e.target;
    if (name === 'image') {
      const file = files[0];
      setFormData(prev => ({ ...prev, image: file }));
      if (file) {
        const reader = new FileReader();
        reader.onloadend = () => setImagePreview(reader.result);
        reader.readAsDataURL(file);
      }
    } else {
      setFormData(prev => ({ ...prev, [name]: value }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      const endpoint = modalType === 'category' ? '/api/categories/products' : '/api/categories/sub';
      const compressedImage = formData.image
        ? await compressImageToHdWebp(formData.image)
        : null;
      
      const data = new FormData();
      data.append('name', formData.name);
      if (formData.slug) data.append('slug', formData.slug);
      if (formData.categoryId) data.append('categoryId', formData.categoryId);
      if (compressedImage) data.append('image', compressedImage);

      if (editingId) {
        await api.put(`${endpoint}/${editingId}`, data, {
          headers: { 'Content-Type': 'multipart/form-data' }
        }); 
      } else {
        await api.post(endpoint, data, {
          headers: { 'Content-Type': 'multipart/form-data' }
        });
      }
      
      setIsModalOpen(false);
      fetchData();
    } catch (err) {
      console.error("Save error:", err);
      alert(err.response?.data?.message || err.response?.data?.error || "Failed to save.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleEdit = (item, type) => {
    setModalType(type);
    setEditingId(item.id || item._id);
    setFormData({
      name: item.name || '',
      slug: item.slug || '',
      categoryId: item.categoryId || '',
      image: null
    });
    setImagePreview(item.imageUrl || null);
    setIsModalOpen(true);
  };

  const handleDelete = async (id, type) => {
    const label = type === 'category' ? 'category' : 'sub-category';
    if (window.confirm(`Are you sure you want to delete this ${label}?`)) {
      try {
        const endpoint = type === 'category' ? `/api/categories/products/${id}` : `/api/categories/sub/${id}`;
        await api.delete(endpoint);
        fetchData();
      } catch (err) {
        alert(`Failed to delete ${label}.`);
        console.error(err);
      }
    }
  };

  const handleAddClick = (type, parentId = '') => {
    setModalType(type);
    setFormData({ ...initialFormState, categoryId: parentId });
    setImagePreview(null);
    setEditingId(null);
    setIsModalOpen(true);
  };

  const filteredCategories = categories.filter(cat => 
    cat.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    subCategories.some(sub => sub.categoryId === (cat.id || cat._id) && sub.name?.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  return (
    <div className="relative flex flex-col h-full p-6">
      
      {/* Header */}
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between mb-6 gap-4">
        <div>
          <h1 className="text-xl font-semibold text-gray-800">Product Categories</h1>
          <p className="text-gray-500 text-[0.85rem] mt-1">Manage hierarchical product organization.</p>
        </div>
        <button 
          onClick={() => handleAddClick('category')}
          className="bg-blue-600 hover:bg-blue-700 text-white rounded text-[0.85rem] font-medium transition-colors shadow-sm px-4 py-2 flex items-center"
        >
          <Plus className="w-4 h-4 mr-1.5" /> Add New Category
        </button>
      </div>

      {/* Search Bar */}
      <div className="bg-white p-4 rounded border border-gray-100 shadow-sm mb-6 flex items-center">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute inset-y-0 left-3 my-auto w-4 h-4 text-gray-400" />
          <input 
            type="text" 
            placeholder="Search categories or sub-categories..." 
            value={searchTerm} 
            onChange={(e) => setSearchTerm(e.target.value)} 
            className="w-full pl-9 px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white" 
          />
        </div>
      </div>

      {error && <p className="mb-4 text-red-500 bg-red-50 px-4 py-3 rounded text-[0.85rem] border border-red-100">{error}</p>}

      {/* Hierarchical List */}
      <div className="bg-white rounded border border-gray-100 shadow-sm overflow-hidden flex flex-col flex-1">
        <div className="overflow-x-auto overflow-y-auto flex-1">
          <table className="w-full text-left border-collapse">
            <thead className="bg-gray-50 border-b border-gray-100 sticky top-0 z-10">
              <tr>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider">Category / Sub-Category</th>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider">Slug</th>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-center tracking-wider w-48">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan="3" className="px-4 py-8 text-center text-[0.85rem] text-gray-500"><Loader2 className="w-6 h-6 animate-spin mx-auto mb-2"/> Loading categories...</td></tr>
              ) : filteredCategories.length === 0 ? (
                <tr><td colSpan="3" className="px-4 py-8 text-center text-[0.85rem] text-gray-500">No categories found.</td></tr>
              ) : (
                filteredCategories.map(cat => {
                    const catId = cat.id || cat._id;
                    const children = subCategories.filter(sub => sub.categoryId === catId);
                    const isExpanded = expandedCategories.has(catId);
                    
                    return (
                        <React.Fragment key={catId}>
                            <tr className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                                <td className="px-4 py-3 text-[0.85rem] text-gray-700 flex items-center gap-2">
                                    <button onClick={() => toggleCategory(catId)} className="p-1 hover:bg-gray-200 rounded text-gray-500">
                                        {isExpanded ? <ChevronDown className="w-4 h-4"/> : <ChevronRight className="w-4 h-4"/>}
                                    </button>
                                    
                                    {cat.imageUrl ? (
                                      <img src={cat.imageUrl} alt={cat.name} className="w-6 h-6 rounded object-cover border border-gray-200" />
                                    ) : (
                                      <Tag className="w-4 h-4 text-gray-400"/>
                                    )}
                                    
                                    <span className="font-medium text-gray-800">{cat.name}</span>
                                    <span className="ml-2 text-[0.7rem] bg-gray-100 text-gray-500 px-1.5 py-0.5 rounded">{children.length} sub</span>
                                </td>
                                <td className="px-4 py-3 text-[0.85rem] text-gray-500 font-mono">/{cat.slug}</td>
                                <td className="px-4 py-3 text-[0.85rem] text-gray-700 text-center">
                                    <div className="flex justify-center gap-1">
                                        <button onClick={() => handleAddClick('subcategory', catId)} title="Add Sub-category" className="p-1.5 text-gray-500 hover:text-green-600 hover:bg-green-50 rounded transition-colors"><Plus className="w-4 h-4" /></button>
                                        <button onClick={() => handleEdit(cat, 'category')} className="p-1.5 text-gray-500 hover:text-blue-600 hover:bg-blue-50 rounded transition-colors"><Edit className="w-4 h-4" /></button>
                                        <button onClick={() => handleDelete(catId, 'category')} className="p-1.5 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded transition-colors"><Trash2 className="w-4 h-4" /></button>
                                    </div>
                                </td>
                            </tr>
                            {isExpanded && children.map(sub => (
                                <tr key={sub.id || sub._id} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                                    <td className="px-4 py-3 text-[0.85rem] text-gray-700 pl-12 flex items-center gap-2">
                                        <div className="w-1.5 h-1.5 rounded-full bg-gray-300"></div>
                                        <span className="text-gray-600">{sub.name}</span>
                                    </td>
                                    <td className="px-4 py-3 text-[0.85rem] text-gray-500 font-mono pl-8">/{sub.slug}</td>
                                    <td className="px-4 py-3 text-[0.85rem] text-gray-700 text-center">
                                        <div className="flex justify-center gap-1">
                                            <button onClick={() => handleEdit(sub, 'subcategory')} className="p-1.5 text-gray-500 hover:text-blue-600 hover:bg-blue-50 rounded transition-colors"><Edit className="w-3.5 h-3.5" /></button>
                                            <button onClick={() => handleDelete(sub.id || sub._id, 'subcategory')} className="p-1.5 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded transition-colors"><Trash2 className="w-3.5 h-3.5" /></button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </React.Fragment>
                    );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* --- ADD / EDIT MODAL --- */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="w-full max-w-lg bg-white rounded shadow-xl flex flex-col max-h-[90vh]">
            
            <div className="p-5 border-b border-gray-100 flex justify-between items-center shrink-0">
              <h2 className="text-xl font-semibold text-gray-800">
                {editingId ? 'Edit ' : 'Add New '}
                {modalType === 'category' ? 'Category' : 'Sub-Category'}
              </h2>
              <button onClick={() => setIsModalOpen(false)} className="text-gray-400 hover:text-gray-600 transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 overflow-y-auto">
              <form id="categoryForm" onSubmit={handleSubmit} className="space-y-4">
                
                {modalType === 'subcategory' && (
                    <div>
                        <label className="block text-[0.85rem] font-medium text-gray-700 mb-1.5">Parent Category <span className="text-red-500">*</span></label>
                        <select 
                            name="categoryId" 
                            required 
                            value={formData.categoryId} 
                            onChange={handleInputChange} 
                            className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white"
                        >
                            <option value="" disabled>Select Parent Category</option>
                            {categories.map(cat => (
                                <option key={cat.id || cat._id} value={cat.id || cat._id}>{cat.name}</option>
                            ))}
                        </select>
                    </div>
                )}

                <div>
                  <label className="block text-[0.85rem] font-medium text-gray-700 mb-1.5">
                    {modalType === 'category' ? 'Category' : 'Sub-Category'} Name <span className="text-red-500">*</span>
                  </label>
                  <input 
                    type="text" 
                    name="name" 
                    required 
                    value={formData.name} 
                    onChange={handleInputChange} 
                    className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white" 
                    placeholder={modalType === 'category' ? "e.g. Laser Machines" : "e.g. Handheld Laser"} 
                  />
                </div>

                {modalType === 'category' && (
                  <div>
                    <label className="block text-[0.85rem] font-medium text-gray-700 mb-1.5">Category Image</label>
                    <div className="mt-1 flex items-center gap-4">
                      {imagePreview && (
                        <img src={imagePreview} alt="Preview" className="w-12 h-12 rounded object-cover border border-gray-200" />
                      )}
                      <input 
                        type="file" 
                        name="image" 
                        accept="image/*" 
                        onChange={handleInputChange} 
                        className="block w-full text-[0.85rem] text-gray-500 file:mr-3 file:py-1.5 file:px-3 file:rounded file:border file:border-gray-200 file:text-[0.85rem] file:font-medium file:bg-white file:text-gray-700 hover:file:bg-gray-50 transition-colors" 
                      />
                    </div>
                  </div>
                )}

              </form>
            </div>

            {/* Footer Buttons */}
            <div className="p-5 border-t border-gray-100 flex justify-end gap-2 shrink-0">
              <button type="button" onClick={() => setIsModalOpen(false)} className="bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-3 py-1.5 font-medium shadow-sm">
                Cancel
              </button>
              <button type="submit" form="categoryForm" disabled={isSubmitting} className="bg-blue-600 hover:bg-blue-700 text-white rounded text-[0.85rem] font-medium transition-colors shadow-sm px-4 py-2 disabled:opacity-70 disabled:cursor-not-allowed">
                {isSubmitting ? 'Saving...' : (editingId ? 'Save Changes' : `Save ${modalType === 'category' ? 'Category' : 'Sub-Category'}`)}
              </button>
            </div>

          </div>
        </div>
      )}

    </div>
  );
}
