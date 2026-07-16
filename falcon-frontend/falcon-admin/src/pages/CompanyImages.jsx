import { useEffect, useMemo, useState } from 'react';
import { ImagePlus, Pencil, Search, Trash2, X } from 'lucide-react';
import api, { getApiData } from '../services/api';
import { useCompany } from '../context/CompanyContext';
import { compressImageToHdWebp } from '../utils/imageCompression';

const INITIAL_FORM = {
  name: '',
  description: '',
};

const CompanyImages = () => {
  const { fetchCompanyData } = useCompany();
  const [images, setImages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [searchTerm, setSearchTerm] = useState('');

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingImage, setEditingImage] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [formData, setFormData] = useState(INITIAL_FORM);
  const [logoFile, setLogoFile] = useState(null);
  const [iconFile, setIconFile] = useState(null);
  const [faviconFile, setFaviconFile] = useState(null);
  const [landingPageFile, setLandingPageFile] = useState(null);

  const [previews, setPreviews] = useState({
    logo: '',
    icon: '',
    favicon: '',
    landingPage: '',
  });

  const fetchCompanyImages = async () => {
    setLoading(true);
    setError('');

    try {
      const response = await api.get('/api/company-images');
      const data = getApiData(response);
      setImages(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Failed to load company images:', err);
      setError(err?.response?.data?.message || 'Failed to load company images.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCompanyImages();
  }, []);

  useEffect(() => {
    const newPreviews = {
      logo: logoFile ? URL.createObjectURL(logoFile) : editingImage?.logoUrl || '',
      icon: iconFile ? URL.createObjectURL(iconFile) : editingImage?.iconUrl || '',
      favicon: faviconFile ? URL.createObjectURL(faviconFile) : editingImage?.faviconUrl || '',
      landingPage: landingPageFile ? URL.createObjectURL(landingPageFile) : editingImage?.landingPageImageUrl || '',
    };

    setPreviews(newPreviews);

    return () => {
      if (logoFile) URL.revokeObjectURL(newPreviews.logo);
      if (iconFile) URL.revokeObjectURL(newPreviews.icon);
      if (faviconFile) URL.revokeObjectURL(newPreviews.favicon);
      if (landingPageFile) URL.revokeObjectURL(newPreviews.landingPage);
    };
  }, [logoFile, iconFile, faviconFile, landingPageFile, editingImage]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((current) => ({ ...current, [name]: value }));
  };

  const handleAddClick = () => {
    setEditingImage(null);
    setFormData(INITIAL_FORM);
    setLogoFile(null);
    setIconFile(null);
    setFaviconFile(null);
    setLandingPageFile(null);
    setSuccess('');
    setError('');
    setIsModalOpen(true);
  };

  const handleEditClick = (image) => {
    setEditingImage(image);
    setFormData({
      name: image.name || '',
      description: image.description || '',
    });
    setLogoFile(null);
    setIconFile(null);
    setFaviconFile(null);
    setLandingPageFile(null);
    setSuccess('');
    setError('');
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingImage(null);
    setFormData(INITIAL_FORM);
    setLogoFile(null);
    setIconFile(null);
    setFaviconFile(null);
    setLandingPageFile(null);
    setError('');
  };

  const handleDeleteClick = async (id) => {
    if (!window.confirm('Are you sure you want to delete this image?')) {
      return;
    }

    try {
      await api.delete(`/api/company-images/${id}`);
      setSuccess('Company image deleted successfully.');
      await fetchCompanyImages();
      await fetchCompanyData();
    } catch (err) {
      console.error('Failed to delete company image:', err);
      setError(err?.response?.data?.message || 'Failed to delete company image.');
    }
  };

  const uploadCompanyImage = async (payload) => {
    const submitData = new FormData();
    if (payload.logo) submitData.append('logo', payload.logo);
    if (payload.icon) submitData.append('icon', payload.icon);
    if (payload.favicon) submitData.append('favicon', payload.favicon);
    if (payload.landingPageImage) submitData.append('landingPageImage', payload.landingPageImage);
    
    submitData.append('name', payload.name);
    submitData.append('description', payload.description);

    const response = await api.post('/api/company-images', submitData);
    return getApiData(response);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError('');
    setSuccess('');

    try {
      const MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5MB limit
      const compressedFiles = {
        logo: logoFile ? await compressImageToHdWebp(logoFile) : null,
        icon: iconFile ? await compressImageToHdWebp(iconFile) : null,
        favicon: faviconFile ? await compressImageToHdWebp(faviconFile) : null,
        landingPageImage: landingPageFile ? await compressImageToHdWebp(landingPageFile) : null,
      };

      const filesToCheck = [
        { name: 'Logo', file: compressedFiles.logo },
        { name: 'Icon', file: compressedFiles.icon },
        { name: 'Favicon', file: compressedFiles.favicon },
        { name: 'Landing Page Image', file: compressedFiles.landingPageImage },
      ];

      for (const item of filesToCheck) {
        if (item.file && item.file.size > MAX_SIZE_BYTES) {
          setError(`${item.name} file size exceeds the 5MB limit. Please select a smaller file.`);
          setIsSubmitting(false);
          return;
        }
      }

      const trimmedPayload = {
        name: formData.name.trim(),
        description: formData.description.trim(),
      };

      if (editingImage) {
        const editingId = editingImage.id || editingImage._id;
        const hasNewFiles = logoFile || iconFile || faviconFile || landingPageFile;

        if (hasNewFiles) {
          const submitData = new FormData();
          if (compressedFiles.logo) submitData.append('logo', compressedFiles.logo);
          if (compressedFiles.icon) submitData.append('icon', compressedFiles.icon);
          if (compressedFiles.favicon) submitData.append('favicon', compressedFiles.favicon);
          if (compressedFiles.landingPageImage) submitData.append('landingPageImage', compressedFiles.landingPageImage);
          
          submitData.append('name', trimmedPayload.name);
          submitData.append('description', trimmedPayload.description);

          await api.put(`/api/company-images/${editingId}`, submitData);
          setSuccess('Company assets updated successfully.');
        } else {
          await api.put(`/api/company-images/${editingId}`, {
            ...editingImage,
            ...trimmedPayload,
          });
          setSuccess('Company details updated successfully.');
        }
      } else {
        if (!logoFile) {
          setError('Please select a logo to upload.');
          setIsSubmitting(false);
          return;
        }

        await uploadCompanyImage({
          ...trimmedPayload,
          logo: compressedFiles.logo,
          icon: compressedFiles.icon,
          favicon: compressedFiles.favicon,
          landingPageImage: compressedFiles.landingPageImage,
        });
        setSuccess('Company assets uploaded successfully.');
      }

      await fetchCompanyImages();
      await fetchCompanyData();
      handleCloseModal();
    } catch (err) {
      console.error('Failed to save company image:', err);
      const serverMessage = err?.response?.data?.message || err?.response?.data?.error;
      if (serverMessage) {
        setError(serverMessage);
      } else if (err?.message === 'Network Error') {
        setError('Network error or connection lost. The files might be too large or the server could be unreachable.');
      } else {
        setError(err?.message || 'Failed to save company image.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const filteredImages = useMemo(() => {
    const normalizedSearch = searchTerm.trim().toLowerCase();

    if (!normalizedSearch) {
      return images;
    }

    return images.filter((image) =>
      [image.name, image.description]
        .filter(Boolean)
        .some((value) => value.toLowerCase().includes(normalizedSearch))
    );
  }, [images, searchTerm]);

  return (
    <div className="relative flex flex-col h-full pb-6 px-6 pt-6">
      <header className="mb-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold text-gray-800">Company Images</h1>
          <p className="text-[0.85rem] text-gray-500 mt-1">
            Manage the company logo and brand image library used across the app.
          </p>
        </div>

        <button
          onClick={handleAddClick}
          className="px-4 py-2 text-white bg-blue-600 rounded hover:bg-blue-700 text-[0.85rem] font-medium shadow-sm transition-colors"
        >
          + Upload New Image
        </button>
      </header>

      <div className="bg-white p-4 rounded border border-gray-100 shadow-sm mb-4 flex items-center">
        <div className="relative flex-1 max-w-md">
          <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
            <Search className="w-4 h-4 text-gray-400" />
          </div>
          <input
            type="text"
            placeholder="Search images by name or description..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="block w-full px-3 py-2 pl-9 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none"
          />
        </div>
        {searchTerm && (
          <button
            onClick={() => setSearchTerm('')}
            className="ml-3 px-3 py-1.5 text-gray-500 bg-gray-50 border border-gray-200 hover:text-red-500 hover:bg-gray-100 rounded text-[0.85rem] flex items-center font-medium transition-colors"
          >
            <X className="w-4 h-4 mr-1.5" /> Clear
          </button>
        )}
      </div>

      {error && <p className="mb-4 text-red-500 bg-red-50 p-3 rounded text-[0.85rem] border border-red-100">{error}</p>}
      {success && <p className="mb-4 text-green-700 bg-green-50 p-3 rounded text-[0.85rem] border border-green-100">{success}</p>}

      <div className="bg-white rounded border border-gray-100 shadow-sm p-6 flex flex-col flex-1">
        {loading ? (
          <div className="text-[0.85rem] text-gray-500">Loading company images...</div>
        ) : filteredImages.length === 0 ? (
          <div className="py-12 text-center text-gray-500">
            <ImagePlus className="w-10 h-10 mx-auto mb-3 text-gray-300" />
            <p className="font-medium text-[0.90rem]">No company images found.</p>
            <p className="text-[0.85rem] mt-1">Upload your first company logo or brand image to get started.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 xl:grid-cols-2 gap-4">
            {filteredImages.map((image) => {
              const imageId = image.id || image._id;

              return (
                <div
                  key={imageId}
                  className="overflow-hidden rounded border border-gray-100 bg-white shadow-sm"
                >
                  <div className="p-4 border-b border-gray-100 bg-gray-50 flex items-center justify-between">
                    <h2 className="text-[0.95rem] font-semibold text-gray-800">
                      {image.name || 'Untitled Branding'}
                    </h2>
                    <div className="flex gap-2">
                      <button
                        onClick={() => handleEditClick(image)}
                        className="px-3 py-1.5 text-[0.75rem] font-medium text-gray-700 bg-white border border-gray-200 rounded hover:bg-gray-50 shadow-sm transition-colors"
                      >
                        <span className="inline-flex items-center gap-1.5">
                          <Pencil className="w-3.5 h-3.5" /> Edit
                        </span>
                      </button>
                      <button
                        onClick={() => handleDeleteClick(imageId)}
                        className="px-3 py-1.5 text-[0.75rem] font-medium text-white bg-red-600 rounded hover:bg-red-700 shadow-sm transition-colors"
                      >
                        <span className="inline-flex items-center gap-1.5">
                          <Trash2 className="w-3.5 h-3.5" /> Delete
                        </span>
                      </button>
                    </div>
                  </div>

                  <div className="p-4">
                    <div className="grid grid-cols-2 gap-4 mb-4">
                      <div className="space-y-1.5">
                        <p className="text-[0.70rem] font-medium text-gray-500 uppercase tracking-wider">Logo</p>
                        <div className="h-24 bg-gray-50 rounded border border-gray-100 overflow-hidden">
                          {image.logoUrl ? (
                            <img src={image.logoUrl} alt="Logo" className="w-full h-full object-contain p-2" />
                          ) : (
                            <div className="w-full h-full flex items-center justify-center text-[0.75rem] text-gray-400">N/A</div>
                          )}
                        </div>
                      </div>
                      <div className="space-y-1.5">
                        <p className="text-[0.70rem] font-medium text-gray-500 uppercase tracking-wider">Icon</p>
                        <div className="h-24 bg-gray-50 rounded border border-gray-100 overflow-hidden">
                          {image.iconUrl ? (
                            <img src={image.iconUrl} alt="Icon" className="w-full h-full object-contain p-2" />
                          ) : (
                            <div className="w-full h-full flex items-center justify-center text-[0.75rem] text-gray-400">N/A</div>
                          )}
                        </div>
                      </div>
                      <div className="space-y-1.5">
                        <p className="text-[0.70rem] font-medium text-gray-500 uppercase tracking-wider">Favicon</p>
                        <div className="h-24 bg-gray-50 rounded border border-gray-100 overflow-hidden">
                          {image.faviconUrl ? (
                            <img src={image.faviconUrl} alt="Favicon" className="w-full h-full object-contain p-2" />
                          ) : (
                            <div className="w-full h-full flex items-center justify-center text-[0.75rem] text-gray-400">N/A</div>
                          )}
                        </div>
                      </div>
                      <div className="space-y-1.5">
                        <p className="text-[0.70rem] font-medium text-gray-500 uppercase tracking-wider">Landing Page Image</p>
                        <div className="h-24 bg-gray-50 rounded border border-gray-100 overflow-hidden">
                          {image.landingPageImageUrl ? (
                            <img src={image.landingPageImageUrl} alt="Landing Page" className="w-full h-full object-cover" />
                          ) : (
                            <div className="w-full h-full flex items-center justify-center text-[0.75rem] text-gray-400">N/A</div>
                          )}
                        </div>
                      </div>
                    </div>

                    <div className="pt-4 border-t border-gray-100">
                      <p className="text-[0.85rem] text-gray-600 line-clamp-2">
                        {image.description || 'No description provided.'}
                      </p>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="w-full max-w-2xl bg-white rounded shadow-xl overflow-hidden flex flex-col max-h-[90vh]">
            <div className="p-5 border-b border-gray-100 flex justify-between items-center shrink-0">
              <h2 className="text-lg font-semibold text-gray-800">
                {editingImage ? 'Edit Company Image' : 'Upload Company Image'}
              </h2>
              <button
                onClick={handleCloseModal}
                className="text-gray-400 hover:text-red-500 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="p-6 overflow-y-auto">
              {error && (
                <div className="mb-4 text-red-500 bg-red-50 p-3 rounded text-[0.85rem] font-medium border border-red-100">
                  {error}
                </div>
              )}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                <div className="md:col-span-2">
                  <label className="block mb-1.5 text-[0.85rem] font-medium text-gray-700">
                    Configuration Name <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    name="name"
                    required
                    value={formData.name}
                    onChange={handleInputChange}
                    className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none transition-colors"
                    placeholder="e.g. Falcon Main Brand Assets"
                  />
                </div>

                <div className="md:col-span-1">
                  <label className="block mb-1.5 text-[0.85rem] font-medium text-gray-700">
                    Logo {!editingImage && <span className="text-red-500">*</span>}
                  </label>
                  <input
                    type="file"
                    accept="image/*"
                    required={!editingImage}
                    onChange={(e) => setLogoFile(e.target.files?.[0] || null)}
                    className="w-full text-[0.80rem] text-gray-500 file:mr-3 file:py-1.5 file:px-3 file:rounded file:border-0 file:text-[0.80rem] file:font-medium file:bg-gray-100 file:text-gray-700 hover:file:bg-gray-200 transition-colors"
                  />
                  <div className="mt-2 h-24 bg-gray-50 border border-gray-200 border-dashed rounded flex items-center justify-center overflow-hidden">
                    {previews.logo ? (
                      <img src={previews.logo} alt="Logo Preview" className="h-full object-contain p-2" />
                    ) : (
                      <span className="text-[0.75rem] text-gray-400">No logo</span>
                    )}
                  </div>
                </div>

                <div className="md:col-span-1">
                  <label className="block mb-1.5 text-[0.85rem] font-medium text-gray-700">Icon</label>
                  <input
                    type="file"
                    accept="image/*"
                    onChange={(e) => setIconFile(e.target.files?.[0] || null)}
                    className="w-full text-[0.80rem] text-gray-500 file:mr-3 file:py-1.5 file:px-3 file:rounded file:border-0 file:text-[0.80rem] file:font-medium file:bg-gray-100 file:text-gray-700 hover:file:bg-gray-200 transition-colors"
                  />
                  <div className="mt-2 h-24 bg-gray-50 border border-gray-200 border-dashed rounded flex items-center justify-center overflow-hidden">
                    {previews.icon ? (
                      <img src={previews.icon} alt="Icon Preview" className="h-full object-contain p-2" />
                    ) : (
                      <span className="text-[0.75rem] text-gray-400">No icon</span>
                    )}
                  </div>
                </div>

                <div className="md:col-span-1">
                  <label className="block mb-1.5 text-[0.85rem] font-medium text-gray-700">Favicon</label>
                  <input
                    type="file"
                    accept="image/*"
                    onChange={(e) => setFaviconFile(e.target.files?.[0] || null)}
                    className="w-full text-[0.80rem] text-gray-500 file:mr-3 file:py-1.5 file:px-3 file:rounded file:border-0 file:text-[0.80rem] file:font-medium file:bg-gray-100 file:text-gray-700 hover:file:bg-gray-200 transition-colors"
                  />
                  <div className="mt-2 h-24 bg-gray-50 border border-gray-200 border-dashed rounded flex items-center justify-center overflow-hidden">
                    {previews.favicon ? (
                      <img src={previews.favicon} alt="Favicon Preview" className="h-full object-contain p-2" />
                    ) : (
                      <span className="text-[0.75rem] text-gray-400">No favicon</span>
                    )}
                  </div>
                </div>

                <div className="md:col-span-1">
                  <label className="block mb-1.5 text-[0.85rem] font-medium text-gray-700">Landing Page Image</label>
                  <input
                    type="file"
                    accept="image/*"
                    onChange={(e) => setLandingPageFile(e.target.files?.[0] || null)}
                    className="w-full text-[0.80rem] text-gray-500 file:mr-3 file:py-1.5 file:px-3 file:rounded file:border-0 file:text-[0.80rem] file:font-medium file:bg-gray-100 file:text-gray-700 hover:file:bg-gray-200 transition-colors"
                  />
                  <div className="mt-2 h-24 bg-gray-50 border border-gray-200 border-dashed rounded flex items-center justify-center overflow-hidden">
                    {previews.landingPage ? (
                      <img src={previews.landingPage} alt="Landing Page Preview" className="h-full object-cover w-full" />
                    ) : (
                      <span className="text-[0.75rem] text-gray-400">No image</span>
                    )}
                  </div>
                </div>

                <div className="md:col-span-2">
                  <label className="block mb-1.5 text-[0.85rem] font-medium text-gray-700">Description</label>
                  <textarea
                    name="description"
                    value={formData.description}
                    onChange={handleInputChange}
                    rows="3"
                    className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none transition-colors resize-none"
                    placeholder="e.g. Primary branding assets used across the platform"
                  ></textarea>
                </div>
              </div>

              <div className="flex justify-end pt-5 mt-5 gap-3 border-t border-gray-100">
                <button
                  type="button"
                  onClick={handleCloseModal}
                  className="px-4 py-2 text-[0.85rem] text-gray-700 bg-white border border-gray-200 rounded hover:bg-gray-50 font-medium transition-colors shadow-sm"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="px-4 py-2 text-[0.85rem] text-white bg-blue-600 rounded hover:bg-blue-700 font-medium transition-colors shadow-sm disabled:opacity-50"
                >
                  {isSubmitting
                    ? 'Saving...'
                    : editingImage
                      ? (logoFile || iconFile || faviconFile || landingPageFile)
                        ? 'Update Assets'
                        : 'Update Details'
                      : 'Upload Branding'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default CompanyImages;
