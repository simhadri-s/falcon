import { useEffect, useState, useRef } from 'react';
import { Search, Plus, Edit, Trash2, Image as ImageIcon, Loader2, X } from 'lucide-react';
import api from '../services/api';
import { compressImageToHdWebp } from '../utils/imageCompression';

export default function AdminIndustries() {
  const [industries, setIndustries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');

  // Modal & Form States
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [editingId, setEditingId] = useState(null);

  // Form State (Text Fields)
  const initialFormState = { name: '', slug: '', description: '' };
  const [formData, setFormData] = useState(initialFormState);
  
  // Image States (File & Preview)
  const [imageFile, setImageFile] = useState(null); // The actual file to upload
  const [imagePreview, setImagePreview] = useState(null); // URL to show preview
  
  const fileInputRef = useRef(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const response = await api.get('/api/industries');
      setIndustries(response.data || []);
    } catch (err) {
      setError("Failed to load industries.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => {
      const newData = { ...prev, [name]: value };
      if (name === 'name' && !editingId) {
        newData.slug = value.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)+/g, '');
      }
      return newData;
    });
  };

  // 🌟 Handle Image File Selection (Local Preview Only)
  const handleImageSelection = (e) => {
    const file = e.target.files[0];
    if (file) {
      setImageFile(file); // Store file for submission
      setImagePreview(URL.createObjectURL(file)); // Show local preview
    }
    e.target.value = null; // Reset input
  };

  // 🌟 Submit Handler (Sending Multipart Form-Data)
  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      // Create FormData object for Multipart upload
      const submitData = new FormData();
      const compressedImage = imageFile
        ? await compressImageToHdWebp(imageFile)
        : null;

      submitData.append('name', formData.name);
      submitData.append('slug', formData.slug);
      submitData.append('description', formData.description || '');
      
      // Append the actual image file under the key 'icon' (Matches your DTO)
      if (compressedImage) {
        submitData.append('icon', compressedImage); 
      }

      if (editingId) {
        await api.put(`/api/industries/${editingId}`, submitData);
      } else {
        await api.post('/api/industries', submitData);
      }
      
      setIsModalOpen(false);
      fetchData();
    } catch (err) {
      console.error("Save error:", err);
      alert(err.response?.data?.message || err.response?.data?.error || "Failed to save industry.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleEdit = (industry) => {
    setEditingId(industry.id || industry._id);
    setFormData({
      name: industry.name || '',
      slug: industry.slug || '',
      description: industry.description || ''
    });
    
    // Set existing image as preview if editing, clear file selection
    setImageFile(null);
    setImagePreview(industry.icon || industry.imageUrl || null); 
    setIsModalOpen(true);
  };

  const handleDelete = async (id) => {
    if (window.confirm("Are you sure you want to delete this industry?")) {
      try {
        await api.delete(`/api/industries/${id}`);
        fetchData();
      } catch (err) {
        alert("Failed to delete industry.");
        console.error(err);
      }
    }
  };

  const handleAddClick = () => {
    setFormData(initialFormState);
    setEditingId(null);
    setImageFile(null);
    setImagePreview(null);
    setIsModalOpen(true);
  };

  const filteredIndustries = industries.filter(ind => 
    ind.name?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="relative flex flex-col h-full p-6">
      
      {/* Header */}
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between mb-6 gap-4">
        <div>
          <h1 className="text-xl font-semibold text-gray-800">Industries</h1>
          <p className="text-[0.85rem] text-gray-500 mt-1">Manage business sectors and categories.</p>
        </div>
        <button 
          onClick={handleAddClick}
          className="bg-blue-600 hover:bg-blue-700 text-white rounded text-[0.85rem] font-medium transition-colors shadow-sm px-4 py-2 flex items-center"
        >
          <Plus className="w-4 h-4 mr-1.5" /> Add New Industry
        </button>
      </div>

      {/* Search Bar */}
      <div className="mb-6">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute inset-y-0 left-3 my-auto w-4 h-4 text-gray-400" />
          <input 
            type="text" 
            placeholder="Search industries by name..." 
            value={searchTerm} 
            onChange={(e) => setSearchTerm(e.target.value)} 
            className="w-full pl-9 pr-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white" 
          />
        </div>
      </div>

      {error && <p className="mb-4 text-[0.85rem] text-red-600 bg-red-50 p-3 rounded border border-red-100">{error}</p>}

      {/* Table */}
      <div className="bg-white rounded border border-gray-100 shadow-sm overflow-hidden flex flex-col flex-1">
        <div className="overflow-x-auto overflow-y-auto flex-1">
          <table className="w-full text-left border-collapse min-w-[800px]">
            <thead className="bg-gray-50 border-b border-gray-100 sticky top-0 z-10">
              <tr>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-center tracking-wider w-24">Icon</th>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider">Industry Name</th>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider">Slug</th>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider">Description</th>
                <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-center tracking-wider w-32">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan="5" className="px-4 py-8 text-center text-[0.85rem] text-gray-500"><Loader2 className="w-6 h-6 animate-spin mx-auto mb-2"/> Loading industries...</td></tr>
              ) : filteredIndustries.length === 0 ? (
                <tr><td colSpan="5" className="px-4 py-8 text-center text-[0.85rem] text-gray-500">No industries found.</td></tr>
              ) : (
                filteredIndustries.map(ind => {
                  const imageSource = ind.iconUrl; 
                  return (
                  <tr key={ind.id || ind._id} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3 text-[0.85rem] text-gray-700 text-center">
                      {imageSource ? (
                        <img src={imageSource} alt={ind.name} className="w-10 h-10 object-cover rounded border border-gray-200 mx-auto" />
                      ) : (
                        <div className="w-10 h-10 bg-gray-50 rounded flex items-center justify-center border border-gray-200 mx-auto text-gray-400">
                          <ImageIcon className="w-5 h-5" />
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-3 text-[0.85rem] text-gray-800 font-medium">{ind.name}</td>
                    <td className="px-4 py-3 text-[0.85rem] text-gray-500 font-mono">/{ind.slug}</td>
                    <td className="px-4 py-3 text-[0.85rem] text-gray-700 line-clamp-2 max-w-sm">{ind.description || '-'}</td>
                    <td className="px-4 py-3 text-[0.85rem] text-gray-700 text-center">
                      <div className="flex justify-center gap-2">
                        <button onClick={() => handleEdit(ind)} className="p-1.5 text-blue-600 hover:bg-blue-50 rounded transition"><Edit className="w-4 h-4" /></button>
                        <button onClick={() => handleDelete(ind.id || ind._id)} className="p-1.5 text-red-600 hover:bg-red-50 rounded transition"><Trash2 className="w-4 h-4" /></button>
                      </div>
                    </td>
                  </tr>
                )
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* --- ADD / EDIT MODAL --- */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="w-full max-w-2xl bg-white rounded shadow-xl flex flex-col max-h-[90vh]">
            
            <div className="p-5 border-b border-gray-100 flex justify-between items-center shrink-0">
              <h2 className="text-xl font-semibold text-gray-800">{editingId ? 'Edit Industry' : 'Add New Industry'}</h2>
              <button onClick={() => setIsModalOpen(false)} className="text-gray-400 hover:text-gray-600 transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 overflow-y-auto">
              <form id="industryForm" onSubmit={handleSubmit} className="space-y-6">
                
                {/* 🌟 File Upload Section */}
                <div className="bg-white p-5 border border-dashed border-gray-200 rounded">
                  <label className="block text-[0.85rem] font-medium text-gray-700 mb-3">Industry Icon / Image</label>
                  <div className="flex items-center gap-5">
                    {/* Preview Area */}
                    <div className="w-20 h-20 shrink-0 bg-gray-50 rounded flex items-center justify-center border border-gray-200 overflow-hidden">
                      {imagePreview ? (
                        <img src={imagePreview} alt="preview" className="w-full h-full object-cover" />
                      ) : (
                        <ImageIcon className="text-gray-300 w-8 h-8" />
                      )}
                    </div>
                    
                    {/* Upload Control */}
                    <div>
                      <button 
                        type="button" 
                        onClick={() => fileInputRef.current.click()} 
                        className="bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-3 py-1.5 font-medium shadow-sm"
                      >
                        Browse Image
                      </button>
                      <p className="text-[0.75rem] text-gray-500 mt-2">Format: JPG, PNG, WEBP</p>
                      
                      <input 
                        type="file" 
                        accept="image/*" 
                        ref={fileInputRef} 
                        onChange={handleImageSelection} 
                        className="hidden" 
                      />
                    </div>
                  </div>
                </div>

                {/* Form Fields */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                  <div>
                    <label className="block text-[0.85rem] font-medium text-gray-700 mb-1">Industry Name <span className="text-red-500">*</span></label>
                    <input type="text" name="name" required value={formData.name} onChange={handleInputChange} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white" placeholder="e.g. Aerospace" />
                  </div>
                  <div>
                    <label className="block text-[0.85rem] font-medium text-gray-700 mb-1">URL Slug <span className="text-red-500">*</span></label>
                    <input type="text" name="slug" required value={formData.slug} onChange={handleInputChange} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white font-mono" placeholder="e.g. aerospace" />
                  </div>
                </div>

                <div>
                  <label className="block text-[0.85rem] font-medium text-gray-700 mb-1">Description</label>
                  <textarea name="description" rows="3" value={formData.description} onChange={handleInputChange} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white resize-none" placeholder="Briefly describe this industry..."></textarea>
                </div>

              </form>
            </div>

            {/* Footer Buttons */}
            <div className="p-5 border-t border-gray-100 bg-gray-50 flex justify-end space-x-3 shrink-0 rounded-b">
              <button type="button" onClick={() => setIsModalOpen(false)} className="bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-3 py-1.5 font-medium shadow-sm">
                Cancel
              </button>
              <button type="submit" form="industryForm" disabled={isSubmitting} className="bg-blue-600 hover:bg-blue-700 text-white rounded text-[0.85rem] font-medium transition-colors shadow-sm px-4 py-2 disabled:opacity-70 disabled:cursor-not-allowed">
                {isSubmitting ? 'Saving...' : (editingId ? 'Update Industry' : 'Save Industry')}
              </button>
            </div>

          </div>
        </div>
      )}

    </div>
  );
}
