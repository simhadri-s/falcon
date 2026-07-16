import { useEffect, useState } from 'react';
import { Search, FilterX, Image as ImageIcon, X } from 'lucide-react';
import api from '../services/api';
import { compressImageToHdWebp } from '../utils/imageCompression';

export default function AdminBanners() {
  const [banners, setBanners] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [searchTerm, setSearchTerm] = useState('');

  // --- NEW: Default Banner Selection State ---
  const [selectedDefaultId, setSelectedDefaultId] = useState(null);

  // Modal & Form State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingBanner, setEditingBanner] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  const [formData, setFormData] = useState({
    title: '', description: '', active: true
  });
  const [imageFile, setImageFile] = useState(null);

  const fetchBanners = async () => {
    setLoading(true);
    try {
      const response = await api.get('/api/banners');
      const fetchedBanners = response.data.data || response.data || [];
      setBanners(fetchedBanners);
      
      // Find the currently active default banner and set its ID to the radio button
      const currentDefault = fetchedBanners.find(b => b.defaultBanner === true);
      if (currentDefault) {
        setSelectedDefaultId(currentDefault.id || currentDefault._id);
      }
    } catch (err) {
      setError('Failed to load banners. Check your backend connection.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBanners();
  }, []);

  // --- NEW: Function to save the default banner ---
  const handleSetDefaultBanner = async () => {
    if (!selectedDefaultId) {
      alert("Please select a banner using the radio buttons first.");
      return;
    }
    
    try {
      // Sending the exact payload you specified via PUT request
      await api.put(`/api/banners/${selectedDefaultId}/default-banner`, { 
        defaultBanner: true 
      });
      alert("Default banner updated successfully!");
      fetchBanners(); // Refresh the list to show the new default tag
    } catch (err) {
      console.error("Failed to update default banner", err);
      alert("Failed to update the default banner. Please check your backend endpoint.");
    }
  };

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData({ ...formData, [name]: type === 'checkbox' ? checked : value });
  };

  const handleAddClick = () => {
    setEditingBanner(null);
    setFormData({ title: '', description: '', active: true });
    setImageFile(null);
    setIsModalOpen(true);
  };

  const handleEditClick = (banner) => {
    setEditingBanner(banner);
    setFormData({
      title: banner.title || '',
      description: banner.description || '',
      active: banner.active !== undefined ? banner.active : true
    });
    setImageFile(null);
    setIsModalOpen(true);
  };

  const handleDeleteClick = async (id) => {
    if (window.confirm("ಖಂಡಿತವಾಗಿಯೂ ಈ ಬ್ಯಾನರ್ ಅನ್ನು ಅಳಿಸಲು ನೀವು ಬಯಸುವಿರಾ? (Are you sure you want to delete this banner?)")) {
      try {
        await api.delete(`/api/banners/${id}`);
        fetchBanners(); 
      } catch (err) { alert("Failed to delete banner."); }
    }
  };

  const handleToggleActive = async (banner) => {
    try {
      const newStatus = !banner.active;
      const bannerId = banner.id || banner._id;
      setBanners(banners.map(b => (b.id || b._id) === bannerId ? { ...b, active: newStatus } : b));
      await api.put(`/api/banners/${bannerId}/status`, { active: newStatus });
    } catch (err) {
      fetchBanners(); 
    }
  };

  const handleCloseModal = () => { setIsModalOpen(false); setEditingBanner(null); };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      const submitData = new FormData();
      const compressedImage = imageFile
        ? await compressImageToHdWebp(imageFile)
        : null;

      submitData.append('title', formData.title);
      submitData.append('description', formData.description); 
      submitData.append('active', formData.active.toString());
      
      if (compressedImage) {
        submitData.append('image', compressedImage);
      }

      const bannerId = editingBanner?.id || editingBanner?._id;
      if (editingBanner) {
        await api.put(`/api/banners/${bannerId}`, submitData);
      } else {
        await api.post('/api/banners', submitData);
      }
      
      fetchBanners();
      handleCloseModal();
    } catch (err) {
      alert("Failed to save banner.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const filteredBanners = banners.filter(b => 
    b.title?.toLowerCase().includes(searchTerm.toLowerCase()) || 
    b.description?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="relative flex flex-col h-full pb-6">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-semibold text-gray-800">Banner Control</h1>
        <button onClick={handleAddClick} className="bg-blue-600 hover:bg-blue-700 text-white rounded text-[0.85rem] font-medium transition-colors shadow-sm px-4 py-2">
          + Add New Banner
        </button>
      </div>

      <div className="bg-white p-4 rounded border border-gray-100 shadow-sm mb-4 flex items-center">
        <div className="relative flex-1 max-w-md">
          <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none"><Search className="w-4 h-4 text-gray-400" /></div>
          <input 
            type="text" 
            placeholder="Search banners by title..." 
            value={searchTerm} 
            onChange={(e) => setSearchTerm(e.target.value)} 
            className="w-full pl-9 px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white" 
          />
        </div>
        {searchTerm && <button onClick={() => setSearchTerm('')} className="ml-3 text-gray-500 hover:text-red-500 flex items-center text-[0.85rem] font-medium"><FilterX className="w-4 h-4 mr-1" /> Clear</button>}
      </div>

      {error && <p className="mb-4 text-red-500 bg-red-50 p-3 rounded text-[0.85rem]">{error}</p>}

      <div className="bg-white rounded border border-gray-100 shadow-sm overflow-auto flex flex-col flex-1">
        <table className="w-full text-left border-collapse">
          <thead className="bg-gray-50 text-gray-600 text-[0.85rem] select-none border-b border-gray-100 sticky top-0 z-10">
            <tr>
              <th className="p-4 font-medium w-48">Image</th>
              <th className="p-4 font-medium">Banner Title & Details</th>
              <th className="p-4 font-medium w-32 text-center border-l border-gray-100">
                <div className="flex flex-col items-center gap-2">
                  <span className="text-[0.75rem] uppercase tracking-wider text-gray-500">Default</span>
                  <button 
                    onClick={handleSetDefaultBanner}
                    className="bg-blue-600 hover:bg-blue-700 text-white rounded text-[0.75rem] font-medium transition-colors shadow-sm px-3 py-1 w-full"
                  >
                    SAVE DEFAULT
                  </button>
                </div>
              </th>
              <th className="p-4 font-medium w-32 text-center">Status</th>
              <th className="p-4 font-medium w-40">Actions</th>
            </tr>
          </thead>
          <tbody className="text-[0.85rem]">
            {loading ? (
              <tr><td colSpan="5" className="p-8 text-center text-gray-500">Loading banners...</td></tr>
            ) : filteredBanners.length === 0 ? (
              <tr><td colSpan="5" className="p-8 text-center text-gray-500">No banners found. Please add one.</td></tr>
            ) : (
              filteredBanners.map((banner) => (
                <tr key={banner.id || banner._id} className={`hover:bg-gray-50 border-b border-gray-100 last:border-0 ${banner.defaultBanner ? 'bg-blue-50/30' : ''}`}>
                  <td className="p-4">
                    {banner.imageUrl ? (
                      <img src={banner.imageUrl} alt={banner.title} className="w-40 h-20 object-cover rounded shadow-sm border border-gray-200" />
                    ) : (
                      <div className="w-40 h-20 bg-gray-50 border border-gray-200 rounded flex items-center justify-center text-gray-400">
                        <ImageIcon className="w-6 h-6 mb-1 opacity-50" />
                      </div>
                    )}
                  </td>
                  
                  <td className="p-4">
                    <div className="font-semibold text-gray-800 text-base">{banner.title || 'Untitled Banner'}</div>
                    <div className="text-gray-500 mt-1 line-clamp-2 max-w-md">{banner.description || 'No description provided.'}</div>
                    {banner.defaultBanner && (
                      <span className="inline-block mt-2 px-2 py-0.5 text-[0.7rem] font-medium text-blue-700 bg-blue-100 rounded-full border border-blue-200">
                        CURRENT DEFAULT
                      </span>
                    )}
                  </td>

                  <td className="p-4 text-center bg-gray-50/50 border-x border-gray-100">
                    <label className="flex justify-center items-center cursor-pointer w-full h-full p-2">
                      <input 
                        type="radio" 
                        name="defaultBannerSelector" 
                        checked={selectedDefaultId === (banner.id || banner._id)}
                        onChange={() => setSelectedDefaultId(banner.id || banner._id)}
                        className="w-4 h-4 text-blue-600 border-gray-300 focus:ring-0 focus:ring-offset-0 cursor-pointer"
                      />
                    </label>
                  </td>

                  <td className="p-4 text-center">
                    <label className="relative inline-flex items-center cursor-pointer">
                      <input type="checkbox" className="sr-only peer" checked={banner.active || false} onChange={() => handleToggleActive(banner)} />
                      <div className="w-9 h-5 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-blue-600"></div>
                    </label>
                  </td>
                  
                  <td className="p-4">
                    <div className="flex flex-wrap gap-2">
                      <button onClick={() => handleEditClick(banner)} className="bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-3 py-1.5 font-medium shadow-sm">Edit</button>
                      <button onClick={() => handleDeleteClick(banner.id || banner._id)} className="bg-white border border-red-200 text-red-600 hover:bg-red-50 rounded text-[0.85rem] px-3 py-1.5 font-medium shadow-sm">Delete</button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="w-full max-w-2xl bg-white rounded shadow-xl overflow-hidden flex flex-col">
            <div className="p-5 border-b border-gray-100 flex justify-between items-center">
              <h2 className="text-xl font-semibold text-gray-800">{editingBanner ? 'Edit Banner' : 'Upload New Banner'}</h2>
              <button onClick={handleCloseModal} className="text-gray-400 hover:text-gray-600 transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>
            
            <form onSubmit={handleSubmit} className="p-5">
              <div className="mb-4">
                <label className="block mb-1 text-[0.85rem] font-medium text-gray-700">Banner Title</label>
                <input 
                  type="text" 
                  name="title" 
                  required 
                  value={formData.title} 
                  onChange={handleInputChange} 
                  className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white" 
                  placeholder="e.g. Summer Sale 2026" 
                />
              </div>

              <div className="mb-4">
                <label className="block mb-1 text-[0.85rem] font-medium text-gray-700">Description</label>
                <textarea 
                  name="description" 
                  value={formData.description} 
                  onChange={handleInputChange} 
                  className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white h-24 resize-y"
                  placeholder="Additional text for the banner..."
                ></textarea>
              </div>

              <div className="mb-6 p-4 border border-dashed border-gray-300 rounded bg-gray-50">
                <label className="block mb-2 text-[0.85rem] font-medium text-gray-700">Banner Image (Desktop / Mobile) <span className="text-red-500">*</span></label>
                <input 
                  type="file" 
                  accept="image/*" 
                  required={!editingBanner} 
                  onChange={(e) => setImageFile(e.target.files[0])} 
                  className="w-full text-[0.85rem] text-gray-500 file:mr-4 file:py-1.5 file:px-3 file:rounded file:border-0 file:text-[0.85rem] file:font-medium file:bg-white file:border-gray-200 file:border file:text-gray-700 hover:file:bg-gray-50 file:shadow-sm" 
                />
                {editingBanner && <p className="text-[0.75rem] text-gray-500 mt-2">Leave blank to keep the existing image.</p>}
              </div>

              <div className="mb-6 flex items-center">
                <input 
                  type="checkbox" 
                  id="active" 
                  name="active" 
                  checked={formData.active} 
                  onChange={handleInputChange} 
                  className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-0 cursor-pointer" 
                />
                <label htmlFor="active" className="ml-2 text-[0.85rem] font-medium text-gray-700 cursor-pointer">Set as Active Banner</label>
              </div>
              
              <div className="flex justify-end pt-4 space-x-3 border-t border-gray-100">
                <button type="button" onClick={handleCloseModal} className="bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-4 py-2 font-medium shadow-sm">Cancel</button>
                <button type="submit" disabled={isSubmitting} className="bg-blue-600 hover:bg-blue-700 text-white rounded text-[0.85rem] font-medium transition-colors shadow-sm px-4 py-2 disabled:opacity-50">
                  {isSubmitting ? 'Saving...' : (editingBanner ? 'Update Banner' : 'Upload Banner')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
