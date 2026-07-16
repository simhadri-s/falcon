import { useEffect, useState, useRef } from 'react';
import { useLocation } from 'react-router-dom';
import { Search, FilterX, ArrowUpDown, ArrowUp, ArrowDown, Download, UploadCloud, X, CheckCircle, AlertCircle, AlertTriangle, Edit, Trash2 } from 'lucide-react';
import api from '../services/api';
import { compressImagesToHdWebp } from '../utils/imageCompression';
import ExpiryDatePicker from '../components/ExpiryDatePicker';
export default function AdminProducts() {
const hasValue = (value) => value !== null && value !== undefined && String(value).trim() !== '';
const toInputPrice = (value) => hasValue(value) ? Number(value).toString() : '';
const formatPrice = (value) => {
const amount = Number(value);
return Number.isFinite(amount) ? `₹${amount.toLocaleString('en-IN')}` : '';
  };
const [products, setProducts] = useState([]);
const [categories, setCategories] = useState([]);
const [availableIndustries, setAvailableIndustries] = useState([]);
const [loading, setLoading] = useState(true);
const [error, setError] = useState('');
const [currentPage, setCurrentPage] = useState(1);
const [totalPages, setTotalPages] = useState(1);
const [limit, setLimit] = useState(10);
const [searchTerm, setSearchTerm] = useState('');
const [filterCategory, setFilterCategory] = useState('');
const [sortConfig, setSortConfig] = useState({ key: null, direction: 'asc' });
const [isModalOpen, setIsModalOpen] = useState(false);
const [editingProduct, setEditingProduct] = useState(null);
const [isSubmitting, setIsSubmitting] = useState(false);
const [activeTab, setActiveTab] = useState('products');
const [formData, setFormData] = useState({
name: '', productCode: '', slug: '', categoryId: '', subCategoryId: '', category: '', description: '', mrp: '', sellingPrice: '', featured: false, published: true, industries: [], specs: [{ key: '', value: '' }],
existingImages: [], manageStock: false, stockQuantity: 0, expiryDate: '',
autoOfferOnExpiry: false, expiryThresholdDays: 7, expiryDiscountPercent: 10.0,
hasVariants: false, variants: []
  });
const [subCategories, setSubCategories] = useState([]);
const [filteredSubCategories, setFilteredSubCategories] = useState([]);
const [newImages, setNewImages] = useState([]);
const [showNewCategory, setShowNewCategory] = useState(false);
const [newCategoryName, setNewCategoryName] = useState('');
const [showNewSubCategory, setShowNewSubCategory] = useState(false);
const [newSubCategoryName, setNewSubCategoryName] = useState('');
const [industrySearch, setIndustrySearch] = useState('');
const [isIndustryDropdownOpen, setIsIndustryDropdownOpen] = useState(false);
const dropdownRef = useRef(null);
const fileInputRef = useRef(null);
const imageInputRef = useRef(null);
const [uploadReport, setUploadReport] = useState(null);
const fetchData = async () => {
setLoading(true);
try {
const params = { page: currentPage, limit: limit };
if (searchTerm) params.search = searchTerm;
if (filterCategory) params.category = filterCategory;
if (sortConfig.key) {
  params.sortBy = sortConfig.key;
  params.sortDirection = sortConfig.direction;
}
const [prodRes, catRes, subCatRes, indRes] = await Promise.all([
api.get('/api/products', { params }),
api.get('/api/categories/products'),
api.get('/api/categories/sub'),
api.get('/api/industries')
]);
setProducts(prodRes.data.data || prodRes.data || []);
setTotalPages(prodRes.data.pages || 1);
setCategories(catRes.data || []);
setSubCategories(subCatRes.data || []);
setAvailableIndustries(indRes.data || []);
    } catch (err) {
setError('Failed to load data. Check your backend connection.');
    } finally {
setLoading(false);
    }
  };
useEffect(() => { setCurrentPage(1); }, [searchTerm, filterCategory, limit]);
useEffect(() => {
const timer = setTimeout(() => { fetchData(); }, 500);
return () => clearTimeout(timer);
  }, [currentPage, searchTerm, filterCategory, limit, sortConfig]);
useEffect(() => {
const handleClickOutside = (event) => {
if (dropdownRef.current && !dropdownRef.current.contains(event.target)) setIsIndustryDropdownOpen(false);
    };
document.addEventListener('mousedown', handleClickOutside);
return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);
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
const sortedProducts = products;
const getSortIcon = (columnName) => {
  if (sortConfig.key !== columnName || !sortConfig.key) return <ArrowUpDown className="w-3 h-3 ml-2 text-gray-300" />;
  return sortConfig.direction === 'asc' ? <ArrowUp className="w-3 h-3 ml-2 text-gray-500" /> : <ArrowDown className="w-3 h-3 ml-2 text-gray-500" />;
};
const downloadSampleCSV = () => {
const csvContent =
      "ProductCode,Name,Category,SubCategory,Description,MRP,SellingPrice,Quantity,Featured,Published,Industries\n" +
"PR2001,Laser Cutter Pro,Electronics,Fiber Laser,High precision cutting tool.,225000,199999,10,TRUE,TRUE,Manufacturing; Automotive\n";
const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
const link = document.createElement("a");
const url = URL.createObjectURL(blob);
link.setAttribute("href", url);
link.setAttribute("download", "Falcon_Product_Bulk_Upload_Template.csv");
document.body.appendChild(link);
link.click();
document.body.removeChild(link);
  };
const handleBulkUpload = async (event) => {
const file = event.target.files[0];
if (!file) return;
if (file.type !== "text/csv" && !file.name.endsWith(".csv")) {
alert("Please upload a valid CSV file.");
event.target.value = '';
return;
    }
const uploadData = new FormData();
uploadData.append("file", file);
setLoading(true);
setUploadReport(null);
try {
const res = await api.post('/api/products/bulk-upload', uploadData);
setUploadReport(res.data);
setCurrentPage(1); fetchData();
    } catch (err) {
      alert(err.response?.data?.message || err.response?.data?.error || "Bulk upload failed.");
    } finally {
setLoading(false);
event.target.value = '';
    }
  };
const handleCreateCategory = async () => {
if (!newCategoryName.trim()) return;
try {
const res = await api.post('/api/categories/products', { name: newCategoryName.trim() });
setCategories([...categories, res.data]);
setFormData({ ...formData, categoryId: res.data.id || res.data._id, category: res.data.name });
setNewCategoryName(''); setShowNewCategory(false);
    } catch (err) { alert("Failed to create category."); }
  };
const handleCreateSubCategory = async () => {
    if (!newSubCategoryName.trim() || !formData.categoryId) return;
    try {
      const res = await api.post('/api/categories/sub', { 
        name: newSubCategoryName.trim(),
        categoryId: formData.categoryId 
      });
      setSubCategories([...subCategories, res.data]);
      setFormData({ ...formData, subCategoryId: res.data.id || res.data._id });
      setNewSubCategoryName(''); setShowNewSubCategory(false);
    } catch (err) { alert("Failed to create sub-category."); }
  };
const handlePrevPage = () => { if (currentPage > 1) setCurrentPage((prev) => prev - 1); };
const handleNextPage = () => { if (currentPage < totalPages) setCurrentPage((prev) => prev + 1); };
const handlePageClick = (pageNum) => setCurrentPage(pageNum);
const handleInputChange = (e) => {
const { name, value, type, checked } = e.target;
setFormData(prev => {
const newData = { ...prev, [name]: type === 'checkbox' ? checked : value };
if (name === 'name') {
newData.slug = value.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)+/g, '');
      }
if (name === 'categoryId') {
newData.subCategoryId = '';
const selectedCat = categories.find(c => c.id === value || c._id === value);
if (selectedCat) newData.category = selectedCat.name;
      }
return newData;
    });
  };
useEffect(() => {
if (formData.categoryId) {
setFilteredSubCategories(subCategories.filter(sc => sc.categoryId === formData.categoryId));
    } else {
setFilteredSubCategories([]);
    }
  }, [formData.categoryId, subCategories]);
const handleAddIndustry = (industryName) => {
if (!formData.industries.includes(industryName)) setFormData({ ...formData, industries: [...formData.industries, industryName] });
setIndustrySearch(''); setIsIndustryDropdownOpen(false);
  };
const handleRemoveIndustry = (industryName) => setFormData({ ...formData, industries: formData.industries.filter(ind => ind !== industryName) });
const filteredIndustries = availableIndustries.filter(ind => ind.name.toLowerCase().includes(industrySearch.toLowerCase()) && !formData.industries.includes(ind.name));
const handleSpecChange = (index, field, value) => {
const newSpecs = [...formData.specs];
newSpecs[index][field] = value;
setFormData({ ...formData, specs: newSpecs });
  };
const addSpecRow = () => setFormData({ ...formData, specs: [...formData.specs, { key: '', value: '' }] });
const removeSpecRow = (index) => setFormData({ ...formData, specs: formData.specs.filter((_, i) => i !== index) });

const addVariantRow = () => setFormData({ ...formData, variants: [...formData.variants, { sku: '', mrp: '', sellingPrice: '', stockQuantity: 0, attributes: [{ key: '', value: '' }] }] });
const removeVariantRow = (index) => setFormData({ ...formData, variants: formData.variants.filter((_, i) => i !== index) });
const handleVariantChange = (index, field, value) => {
  const newVariants = [...formData.variants];
  newVariants[index][field] = value;
  setFormData({ ...formData, variants: newVariants });
};
const addVariantAttr = (vIndex) => {
  const newVariants = [...formData.variants];
  if (!newVariants[vIndex].attributes) newVariants[vIndex].attributes = [];
  newVariants[vIndex].attributes.push({ key: '', value: '' });
  setFormData({ ...formData, variants: newVariants });
};
const removeVariantAttr = (vIndex, aIndex) => {
  const newVariants = [...formData.variants];
  newVariants[vIndex].attributes = newVariants[vIndex].attributes.filter((_, i) => i !== aIndex);
  setFormData({ ...formData, variants: newVariants });
};
const handleVariantAttrChange = (vIndex, aIndex, field, value) => {
  const newVariants = [...formData.variants];
  newVariants[vIndex].attributes[aIndex][field] = value;
  setFormData({ ...formData, variants: newVariants });
};
const handleImageSelection = (e) => {
const files = Array.from(e.target.files);
if (!files.length) return;
const newImageObjects = files.map(file => ({
file: file,
previewUrl: URL.createObjectURL(file)
    }));
setNewImages(prev => [...prev, ...newImageObjects]);
e.target.value = null;
  };
const handleRemoveExistingImage = (index) => {
setFormData(prev => ({ ...prev, existingImages: prev.existingImages.filter((_, i) => i !== index) }));
  };
const handleRemoveNewImage = (index) => {
setNewImages(prev => {
const updated = [...prev];
URL.revokeObjectURL(updated[index].previewUrl);
updated.splice(index, 1);
return updated;
    });
  };
const handleAddClick = () => {
setEditingProduct(null);
setFormData({ name: '', productCode: '', slug: '', categoryId: '', subCategoryId: '', category: '', description: '', mrp: '', sellingPrice: '', featured: false, published: true, industries: [], specs: [{ key: '', value: '' }], existingImages: [], manageStock: false, stockQuantity: 0, expiryDate: '', autoOfferOnExpiry: false, expiryThresholdDays: 7, expiryDiscountPercent: 10.0, hasVariants: false, variants: [] });
setNewImages([]); setShowNewCategory(false); setShowNewSubCategory(false); setIndustrySearch(''); setIsModalOpen(true);
  };
const handleEditClick = (product) => {
setEditingProduct(product);
setFormData({
name: product.name || '',
productCode: product.productCode || '',
slug: product.slug || '',
category: product.category || '',
categoryId: product.categoryId || '',
subCategoryId: product.subCategoryId || '',
description: product.description || '',
mrp: toInputPrice(product.mrp),
sellingPrice: toInputPrice(product.sellingPrice),
featured: product.featured || false,
published: product.published !== undefined ? product.published : true,
industries: product.industries ? product.industries.map(i => typeof i === 'string' ? i : i.name) : [],
specs: product.specs && product.specs.length > 0 ? product.specs : [{ key: '', value: '' }],
existingImages: product.imageUrls || [],
manageStock: product.manageStock || false,
stockQuantity: product.stockQuantity || 0,
expiryDate: product.expiryDate || '',
autoOfferOnExpiry: product.autoOfferOnExpiry || false,
expiryThresholdDays: product.expiryThresholdDays || 7,
expiryDiscountPercent: product.expiryDiscountPercent || 10.0,
hasVariants: product.hasVariants || false,
variants: product.variants ? product.variants.map(v => {
  const attrs = [];
  if (v.attributes) {
    Object.keys(v.attributes).forEach(key => {
      attrs.push({ key, value: v.attributes[key] });
    });
  }
  if (attrs.length === 0) attrs.push({ key: '', value: '' });
  return { ...v, mrp: toInputPrice(v.mrp), sellingPrice: toInputPrice(v.sellingPrice), attributes: attrs };
}) : []
  });
setNewImages([]); setShowNewCategory(false); setShowNewSubCategory(false); setIndustrySearch(''); setIsModalOpen(true);
};
const location = useLocation();
useEffect(() => {
if (location.state?.editProduct) {
handleEditClick(location.state.editProduct);
window.history.replaceState({}, document.title);
  }
}, [location.state]);
const handleDeleteClick = async (id) => {
if (window.confirm("Are you sure you want to delete this product?")) {
try { await api.delete(`/api/products/${id}`); fetchData(); } 
      catch (err) { alert("Failed to delete product."); }
  }
};
const handleToggleFeatured = async (product) => {
try {
const newStatus = !product.featured;
const productId = product.id || product._id;
setProducts(products.map(p => (p.id || p._id) === productId ? { ...p, featured: newStatus } : p));
      // Sending toggle to backend
await api.patch(`/api/products/${productId}/toggle-featured`, { featured: newStatus });
  } catch (err) { fetchData(); }
};
const handleTogglePublish = async (product) => {
try {
const productId = product.id || product._id;
const newStatus = !product.published;
setProducts(products.map(p => (p.id || p._id) === productId ? { ...p, published: newStatus } : p));
await api.patch(`/api/products/${productId}/toggle-publish`);
  } catch (err) { fetchData(); }
};
const handleToggleAutoOffer = async (product) => {
  try {
    const productId = product.id || product._id;
    const newStatus = !product.autoOfferOnExpiry;
    setProducts(products.map(p => (p.id || p._id) === productId ? { ...p, autoOfferOnExpiry: newStatus } : p));
    await api.patch(`/api/products/${productId}/stock-and-price`, { autoOfferOnExpiry: newStatus });
  } catch (err) { fetchData(); }
};
const handleCloseModal = () => { setIsModalOpen(false); setEditingProduct(null); };
const handleSubmit = async (e) => {
e.preventDefault();
const hasMrp = String(formData.mrp).trim() !== '';
const hasSellingPrice = String(formData.sellingPrice).trim() !== '';
if (hasMrp && hasSellingPrice && Number(formData.sellingPrice) > Number(formData.mrp)) {
alert('Selling price cannot be greater than MRP.');
return;
  }
setIsSubmitting(true);
try {
const submitData = new FormData();
const compressedImages = await compressImagesToHdWebp(newImages.map(imgObj => imgObj.file));
submitData.append('name', formData.name);
submitData.append('productCode', formData.productCode);
submitData.append('slug', formData.slug);
submitData.append('categoryId', formData.categoryId);
submitData.append('subCategoryId', formData.subCategoryId);
submitData.append('category', formData.category);
submitData.append('description', formData.description);
submitData.append('mrp', String(formData.mrp ?? '').trim());
submitData.append('sellingPrice', String(formData.sellingPrice ?? '').trim());
submitData.append('featured', formData.featured.toString());
submitData.append('published', formData.published.toString());
submitData.append('manageStock', formData.manageStock.toString());
submitData.append('stockQuantity', (formData.stockQuantity ?? 0).toString());
submitData.append('expiryDate', formData.expiryDate || '');
submitData.append('autoOfferOnExpiry', formData.autoOfferOnExpiry.toString());
submitData.append('expiryThresholdDays', (formData.expiryThresholdDays ?? 7).toString());
submitData.append('expiryDiscountPercent', (formData.expiryDiscountPercent ?? 10).toString());
const selectedSlugs = formData.industries.map(name => {
  const match = availableIndustries.find(ind => ind.name === name);
  return match ? match.slug : name.toLowerCase().replace(/[^a-z0-9]+/g, '-');
});
selectedSlugs.forEach(slug => {
  submitData.append('industries', slug);
});
const validSpecs = formData.specs.filter(spec => spec.key.trim() !== '' && spec.value.trim() !== '');
submitData.append('specsJson', JSON.stringify(validSpecs));
submitData.append('existingImageUrls', JSON.stringify(formData.existingImages));
submitData.append('hasVariants', Boolean(formData.hasVariants).toString());
if (formData.hasVariants && formData.variants) {
  const variantsPayload = formData.variants.map(v => {
    const attrMap = {};
    if (v.attributes) {
      v.attributes.forEach(attr => {
        if (attr.key && attr.key.trim() !== '') {
          attrMap[attr.key.trim()] = attr.value.trim();
        }
      });
    }
    return {
      sku: v.sku,
      mrp: v.mrp ? Number(v.mrp) : null,
      sellingPrice: v.sellingPrice ? Number(v.sellingPrice) : null,
      stockQuantity: v.stockQuantity ? Number(v.stockQuantity) : 0,
      attributes: attrMap
    };
  });
  submitData.append('variantsJson', JSON.stringify(variantsPayload));
}

compressedImages.forEach((file) => {
submitData.append('images', file);
    });
const productId = editingProduct?.id || editingProduct?._id;
if (editingProduct) await api.put(`/api/products/${productId}`, submitData);
      else { await api.post('/api/products', submitData); setCurrentPage(1); }
fetchData(); handleCloseModal();
  } catch (err) {
console.error("Save Error:", err.response || err);
if (err.response && err.response.data && typeof err.response.data.message === 'string' && err.response.data.message.includes('duplicate key')) {
alert("Error: This Product Code or Slug already exists! Please use unique values.");
    } else {
alert(err.response?.data?.message || "Failed to save product. Please check your inputs.");
    }
  } finally { setIsSubmitting(false); }
};
const clearFilters = () => { setSearchTerm(''); setFilterCategory(''); setLimit(10); };
return (
    <div className="bg-white p-6 rounded-lg shadow-sm min-h-screen font-sans">
      <div className="flex flex-col lg:flex-row lg:items-center justify-between mb-5 gap-4">
        <h1 className="text-[1.1rem] font-medium text-gray-800">Products</h1>
        <div className="flex flex-wrap items-center gap-3">
          <button onClick={downloadSampleCSV} className="flex items-center px-4 py-1.5 text-[0.85rem] font-medium text-gray-600 bg-white border border-gray-200 rounded hover:bg-gray-50 shadow-sm transition-colors" title="Download CSV Template">
            <Download className="w-4 h-4 mr-1.5" /> Template
          </button>
          <button onClick={() => fileInputRef.current.click()} className="flex items-center px-4 py-1.5 text-[0.85rem] font-medium text-gray-600 bg-white border border-gray-200 rounded hover:bg-gray-50 shadow-sm transition-colors">
            <UploadCloud className="w-4 h-4 mr-1.5" /> Bulk Upload
          </button>
          <input type="file" accept=".csv" ref={fileInputRef} className="hidden" onChange={handleBulkUpload} />
          <button onClick={handleAddClick} className="bg-[#2563eb] hover:bg-blue-600 text-white px-4 py-1.5 rounded text-[0.85rem] font-medium transition-colors shadow-sm">
            Add New Product
          </button>
        </div>
      </div>
      <div className="flex flex-col md:flex-row gap-4 items-center justify-between mb-6">
        <div className="flex items-center gap-3 w-full md:w-auto">
          <div className="relative">
            <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 transform -translate-y-1/2" />
            <input type="text" placeholder="Search products..." value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} className="pl-9 pr-4 py-1.5 border border-gray-200 rounded text-[0.85rem] outline-none focus:border-blue-400 w-64" />
          </div>
          <select value={filterCategory} onChange={(e) => setFilterCategory(e.target.value)} className="border border-gray-200 rounded px-3 py-1.5 text-[0.85rem] text-gray-500 outline-none focus:border-blue-400 bg-white">
            <option value="">All Categories</option>
            {categories.map(cat => <option key={cat.id || cat._id} value={cat.id || cat._id}>{cat.name}</option>)}
          </select>
          {(searchTerm || filterCategory) && <button onClick={clearFilters} className="text-gray-500 hover:text-red-500 flex items-center text-[0.85rem] font-medium"><FilterX className="w-4 h-4 mr-1" /> Clear</button>}
        </div>
        <div className="w-full md:w-auto flex items-center gap-2 border-l pl-4 border-gray-200">
          <span className="text-[0.85rem] text-gray-600">Show :</span>
          <select value={limit} onChange={(e) => setLimit(Number(e.target.value))} className="border border-gray-200 rounded px-3 py-1.5 text-[0.85rem] text-gray-500 outline-none focus:border-blue-400 bg-white">
            <option value={10}>10</option>
            <option value={20}>20</option>
            <option value={50}>50</option>
            <option value={100}>100</option>
          </select>
        </div>
      </div>
      {/* Tabs Layout */}
      <div className="flex border-b border-gray-200 mb-6 gap-6">
        <button
          onClick={() => { setActiveTab('products'); clearFilters(); }}
          className={`pb-2 text-[0.85rem] font-medium transition-all duration-200 ${
            activeTab === 'products'
              ? 'border-b-2 border-blue-600 text-blue-600'
              : 'text-gray-500 hover:text-gray-800'
          }`}
        >
          Product List
        </button>
        <button
          onClick={() => { setActiveTab('inventory'); clearFilters(); }}
          className={`pb-2 text-[0.85rem] font-medium transition-all duration-200 ${
            activeTab === 'inventory'
              ? 'border-b-2 border-blue-600 text-blue-600'
              : 'text-gray-500 hover:text-gray-800'
          }`}
        >
          Stock & Price Management
        </button>
        <button
          onClick={() => { setActiveTab('expiry-offers'); clearFilters(); }}
          className={`pb-2 text-[0.85rem] font-medium transition-all duration-200 ${
            activeTab === 'expiry-offers'
              ? 'border-b-2 border-[#E8A020] text-[#E8A020]'
              : 'text-gray-500 hover:text-gray-800'
          }`}
        >
          Manage Expiry Offer
        </button>
      </div>
      {error && <p className="mb-4 text-red-500 bg-red-50 p-3 rounded">{error}</p>}
      <div className="bg-white rounded shadow">
        {activeTab === 'products' ? (
          <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse min-w-[900px]">
            <thead>
              <tr>
                <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap w-24">Image</th>
                <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap cursor-pointer hover:text-blue-600 transition-colors" onClick={() => handleSort('productCode')}>
                  <div className="flex items-center">Code {getSortIcon('productCode')}</div>
                </th>
                <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap cursor-pointer hover:text-blue-600 transition-colors" onClick={() => handleSort('name')}>
                  <div className="flex items-center">Name {getSortIcon('name')}</div>
                </th>
                <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap cursor-pointer hover:text-blue-600 transition-colors" onClick={() => handleSort('category')}>
                  <div className="flex items-center">Category {getSortIcon('category')}</div>
                </th>
                <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap">
                  <div className="flex items-center">Sub-Category</div>
                </th>
                <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap cursor-pointer hover:text-blue-600 transition-colors" onClick={() => handleSort('sellingPrice')}>
                  <div className="flex items-center">Price {getSortIcon('sellingPrice')}</div>
                </th>
                <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap cursor-pointer hover:text-blue-600 transition-colors" onClick={() => handleSort('featured')}>
                  <div className="flex items-center">Featured {getSortIcon('featured')}</div>
                </th>
                <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap cursor-pointer hover:text-blue-600 transition-colors" onClick={() => handleSort('autoOfferOnExpiry')}>
                  <div className="flex items-center">Auto Offer {getSortIcon('autoOfferOnExpiry')}</div>
                </th>
                <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap cursor-pointer hover:text-blue-600 transition-colors" onClick={() => handleSort('published')}>
                  <div className="flex items-center">Published {getSortIcon('published')}</div>
                </th>
                <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap w-32">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
<tr><td colSpan="8" className="p-8 text-center text-gray-500">Loading products...</td></tr>
              ) : sortedProducts.length === 0 ? (
                <tr><td colSpan="8" className="p-8 text-center text-gray-500">No products found.</td></tr>
              ) : (
                sortedProducts.map((product) => (
                  <tr key={product.id || product._id} className="hover:bg-gray-50">
                    <td className="p-4 border-b border-gray-100"><ProductImageCarousel images={product.imageUrls} productName={product.name} /></td>
                    <td className="p-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-700">
                      {product.productCode ? product.productCode : <span className="text-gray-400">-</span>}
                    </td>
                    <td className="p-4 font-medium border-b border-gray-100 text-[0.85rem] text-gray-800">
                      <div className="flex flex-wrap items-center gap-2">
                        <span>{product.name}</span>
                        {(product.stockQuantity !== null && product.stockQuantity !== undefined ? Number(product.stockQuantity) : 0) < 20 && (
                          <span className="px-1.5 py-0.5 text-[0.7rem] font-medium text-red-600 border border-red-200 rounded">
                            Low Stock ({product.stockQuantity || 0})
                          </span>
                        )}
                        {product.isOnExpiryOffer && (
                          <span className="px-1.5 py-0.5 text-[0.7rem] font-medium text-orange-600 border border-orange-200 rounded">
                            SALE
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="p-4 border-b border-gray-100 text-[0.85rem] text-gray-600">{product.categoryName || product.category}</td>
                    <td className="p-4 border-b border-gray-100 text-[0.85rem] text-gray-600">{product.subCategoryName || '-'}</td>
                    <td className="p-4 border-b border-gray-100 text-[0.85rem]">
                      {(() => {
                        const hasSelling = hasValue(product.sellingPrice) && Number(product.sellingPrice) > 0;
                        return hasSelling || hasValue(product.mrp) ? (
                          <div className="flex items-center gap-2">
                            <span className="font-semibold text-gray-800">
                              {formatPrice(Number(hasSelling ? product.sellingPrice : product.mrp))}
                            </span>
                            {hasSelling && hasValue(product.mrp) && Number(product.mrp) > Number(product.sellingPrice) ? (
                              <span className="text-xs text-gray-400 line-through">
                                {formatPrice(Number(product.mrp))}
                              </span>
                            ) : null}
                          </div>
                        ) : (
                          <span className="text-gray-400 text-sm">-</span>
                        );
                      })()}
                    </td>
                    <td className="p-4 border-b border-gray-100">
                      <label className="relative inline-flex items-center cursor-pointer">
                        <input type="checkbox" className="sr-only peer" checked={product.featured || false} onChange={() => handleToggleFeatured(product)} />
                        <div className="w-8 h-4 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-3 after:w-3 after:transition-all peer-checked:bg-[#2563eb]"></div>
                      </label>
                    </td>
                    <td className="p-4 border-b border-gray-100">
                      <label className="relative inline-flex items-center cursor-pointer">
                        <input type="checkbox" className="sr-only peer" checked={product.autoOfferOnExpiry || false} onChange={() => handleToggleAutoOffer(product)} />
                        <div className="w-8 h-4 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-3 after:w-3 after:transition-all peer-checked:bg-[#2563eb]"></div>
                      </label>
                    </td>
                    <td className="p-4 border-b border-gray-100">
                      <label className="relative inline-flex items-center cursor-pointer">
                        <input type="checkbox" className="sr-only peer" checked={product.published || false} onChange={() => handleTogglePublish(product)} />
                        <div className="w-8 h-4 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-3 after:w-3 after:transition-all peer-checked:bg-green-500"></div>
                      </label>
                    </td>
                    <td className="p-4 border-b border-gray-100">
                      <div className="flex items-center gap-3">
                        <button onClick={() => handleEditClick(product)} className="text-gray-500 hover:text-blue-600 transition-colors flex items-center gap-1 text-[0.85rem] font-medium"><Edit className="w-4 h-4" /> Edit</button>
                        <button onClick={() => handleDeleteClick(product.id || product._id)} className="text-gray-500 hover:text-red-600 transition-colors flex items-center gap-1 text-[0.85rem] font-medium"><Trash2 className="w-4 h-4" /> Delete</button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      ) : activeTab === 'inventory' ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse min-w-[1000px]">
              <thead>
                <tr>
                  <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap w-24">Image</th>
                  <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap w-36">Code</th>
                  <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap">Name</th>
                  <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap w-44">MRP (₹)</th>
                  <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap w-44">Selling Price (₹)</th>
                  <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap w-36">Manage Stock</th>
                  <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap w-40">Stock Qty</th>
                  <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap w-44">Expiry Date</th>
                  <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap w-32">Actions</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr><td colSpan="9" className="p-8 text-center text-gray-500">Loading inventory details...</td></tr>
                ) : sortedProducts.length === 0 ? (
                  <tr><td colSpan="9" className="p-8 text-center text-gray-500">No products found.</td></tr>
                ) : (
                  sortedProducts.map((product) => (
                    <InventoryRow key={product.id || product._id} product={product} onSaveSuccess={fetchData} />
                  ))
                )}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse min-w-[1100px]">
              <thead>
                <tr>
                  <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap w-24">Image</th>
                  <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap w-36">Code</th>
                  <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap">Name</th>
                  <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap w-40">Expiry Date</th>
                  <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap w-32 text-center">Auto Offer</th>
                  <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap w-40 text-center">Threshold (Days)</th>
                  <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap w-40 text-center">Discount (%)</th>
                  <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap w-44">Status</th>
                  <th className="py-4 px-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-800 whitespace-nowrap w-32">Actions</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr><td colSpan="9" className="p-8 text-center text-gray-500">Loading expiry offers...</td></tr>
                ) : sortedProducts.length === 0 ? (
                  <tr><td colSpan="9" className="p-8 text-center text-gray-500">No products found.</td></tr>
                ) : (
                  sortedProducts.map((product) => (
                    <ExpiryOfferRow 
                      key={product.id || product._id} 
                      product={product} 
                      onSaveSuccess={fetchData} 
                      onToggleAutoOffer={() => handleToggleAutoOffer(product)}
                    />
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
        {!loading && products.length > 0 && (
          <div className="flex items-center justify-between pt-4 mt-4 text-[0.85rem] text-gray-500">
            <div>
              Showing <span className="font-medium text-gray-900">{currentPage}</span> to <span className="font-medium text-gray-900">{totalPages}</span> of entries
            </div>
            <div className="flex items-center gap-1 border border-gray-200 rounded overflow-hidden">
              <button onClick={handlePrevPage} disabled={currentPage === 1} className={`px-3 py-1.5 transition-colors ${currentPage === 1 ? 'text-gray-300 bg-gray-50 cursor-not-allowed' : 'text-gray-600 hover:bg-gray-50 bg-white'}`}>Previous</button>
              {[...Array(totalPages)].map((_, index) => (
                <button key={index + 1} onClick={() => handlePageClick(index + 1)} className={`px-3 py-1.5 transition-colors ${currentPage === index + 1 ? 'bg-blue-50 text-blue-600 font-medium' : 'text-gray-600 hover:bg-gray-50 bg-white'}`}>
                  {index + 1}
                </button>
              ))}
              <button onClick={handleNextPage} disabled={currentPage === totalPages} className={`px-3 py-1.5 transition-colors ${currentPage === totalPages ? 'text-gray-300 bg-gray-50 cursor-not-allowed' : 'text-gray-600 hover:bg-gray-50 bg-white'}`}>Next</button>
            </div>
          </div>
        )}
      </div>
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="w-full max-w-4xl bg-white rounded shadow-2xl overflow-hidden flex flex-col max-h-[95vh]">
            <div className="p-6 border-b border-gray-100 flex justify-between items-center">
              <h2 className="text-lg font-semibold text-gray-800">{editingProduct ? 'Edit Product' : 'Add New Product'}</h2>
              <button onClick={handleCloseModal} className="text-gray-400 hover:text-red-500 transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>
            <form onSubmit={handleSubmit} className="p-6 overflow-y-auto flex-1">
              <div className="grid grid-cols-2 gap-6 mb-6">
                <div className="col-span-2 md:col-span-1">
                  <label className="block mb-1.5 text-[0.80rem] font-medium text-gray-600">Product Name <span className="text-red-500">*</span></label>
                  <input type="text" name="name" required value={formData.name} onChange={handleInputChange} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none" />
                </div>
                <div className="col-span-2 md:col-span-1">
                  <label className="block mb-1.5 text-[0.80rem] font-medium text-gray-600">Product Code</label>
                  <input type="text" name="productCode" value={formData.productCode} onChange={handleInputChange} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none" placeholder="e.g. FL-100 (Optional)" />
                </div>
                <div className="col-span-2 md:col-span-1">
                  <label className="block mb-1.5 text-[0.80rem] font-medium text-gray-600">URL Slug (Auto-generated)</label>
                  <input type="text" name="slug" value={formData.slug} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-gray-50 text-gray-500 cursor-not-allowed" disabled readOnly placeholder="Auto-generated from name" />
                </div>
                <div className="col-span-2 md:col-span-1">
                  <label className="block mb-1.5 text-[0.80rem] font-medium text-gray-600">Category <span className="text-red-500">*</span></label>
                  {!showNewCategory ? (
                    <div className="flex gap-2">
                      <select name="categoryId" required value={formData.categoryId} onChange={handleInputChange} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white">
                        <option value="" disabled>Select Category</option>
                        {categories.map(cat => <option key={cat.id || cat._id} value={cat.id || cat._id}>{cat.name}</option>)}
                      </select>
                      <button type="button" onClick={() => setShowNewCategory(true)} className="px-4 bg-gray-50 border border-gray-200 text-gray-600 hover:bg-gray-100 rounded transition-colors">+</button>
                    </div>
                  ) : (
                    <div className="flex gap-2">
                      <input type="text" placeholder="New category name" value={newCategoryName} onChange={(e) => setNewCategoryName(e.target.value)} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none" autoFocus />
                      <button type="button" onClick={handleCreateCategory} className="px-4 bg-green-600 hover:bg-green-700 text-white rounded text-[0.85rem] font-medium transition-colors">Save</button>
                      <button type="button" onClick={() => setShowNewCategory(false)} className="px-3 bg-red-50 hover:bg-red-100 text-red-600 rounded border border-red-200 transition-colors"><X className="w-4 h-4" /></button>
                    </div>
                  )}
                </div>
                <div className="col-span-2 md:col-span-1">
                  <label className="block mb-1.5 text-[0.80rem] font-medium text-gray-600">Sub-Category</label>
                  {!showNewSubCategory ? (
                    <div className="flex gap-2">
                      <select name="subCategoryId" value={formData.subCategoryId} onChange={handleInputChange} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white disabled:bg-gray-50" disabled={!formData.categoryId}>
                        <option value="">Select Sub-Category (Optional)</option>
                        {filteredSubCategories.map(sc => <option key={sc.id || sc._id} value={sc.id || sc._id}>{sc.name}</option>)}
                      </select>
                      <button type="button" onClick={() => setShowNewSubCategory(true)} disabled={!formData.categoryId} className="px-4 bg-gray-50 border border-gray-200 text-gray-600 hover:bg-gray-100 rounded transition-colors disabled:opacity-50">+</button>
                    </div>
                  ) : (
                    <div className="flex gap-2">
                      <input type="text" placeholder="New sub-category name" value={newSubCategoryName} onChange={(e) => setNewSubCategoryName(e.target.value)} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none" autoFocus />
                      <button type="button" onClick={handleCreateSubCategory} className="px-4 bg-green-600 hover:bg-green-700 text-white rounded text-[0.85rem] font-medium transition-colors">Save</button>
                      <button type="button" onClick={() => setShowNewSubCategory(false)} className="px-3 bg-red-50 hover:bg-red-100 text-red-600 rounded border border-red-200 transition-colors"><X className="w-4 h-4" /></button>
                    </div>
                  )}
                </div>
                <div className="col-span-2">
                  <label className="block mb-1.5 text-[0.80rem] font-medium text-gray-600">Description</label>
                  <textarea name="description" value={formData.description} onChange={handleInputChange} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded h-24 focus:ring-0 focus:border-blue-400 outline-none"></textarea>
                </div>
                <div className="col-span-2 md:col-span-1">
                  <label className="block mb-1.5 text-[0.80rem] font-medium text-gray-600">MRP</label>
                  <input type="number" name="mrp" min="0" step="0.01" value={formData.mrp} onChange={handleInputChange} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none" placeholder="e.g. 225000" autoComplete="off" onWheel={(e) => e.target.blur()} />
                </div>
                <div className="col-span-2 md:col-span-1">
                  <label className="block mb-1.5 text-[0.80rem] font-medium text-gray-600">Selling Price</label>
                  <input type="number" name="sellingPrice" min="0" step="0.01" value={formData.sellingPrice} onChange={handleInputChange} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none" placeholder="e.g. 199999" autoComplete="off" onWheel={(e) => e.target.blur()} />
                </div>
                <div className="col-span-2" ref={dropdownRef}>
                  <label className="block mb-1.5 text-[0.80rem] font-medium text-gray-600">Select Industries</label>
                  <div className="relative">
                    <div className="flex flex-wrap items-center gap-2 p-2 border border-gray-200 rounded bg-white min-h-[42px] focus-within:border-blue-400 transition-colors">
                      {formData.industries.map((indName) => (
                        <span key={indName} className="flex items-center gap-1 px-3 py-1 text-[0.80rem] font-medium text-gray-700 bg-gray-100 border border-gray-200 rounded-full">
                          {indName}
                          <button type="button" onClick={() => handleRemoveIndustry(indName)} className="ml-1 text-gray-400 hover:text-red-500 font-bold focus:outline-none">&times;</button>
                        </span>
                      ))}
                      <input type="text" placeholder={formData.industries.length === 0 ? "Search and select industries..." : ""} value={industrySearch} onChange={(e) => { setIndustrySearch(e.target.value); setIsIndustryDropdownOpen(true); }} onFocus={() => setIsIndustryDropdownOpen(true)} className="flex-1 min-w-[150px] outline-none bg-transparent text-[0.85rem] text-gray-700" />
                    </div>
                    {isIndustryDropdownOpen && (
                      <div className="absolute z-10 w-full mt-1 bg-white border border-gray-200 rounded shadow-lg max-h-48 overflow-y-auto">
                        {filteredIndustries.length > 0 ? (
                          filteredIndustries.map((ind) => (
                            <div key={ind.id || ind._id} onClick={() => handleAddIndustry(ind.name)} className="px-4 py-2 text-sm cursor-pointer hover:bg-blue-50 text-gray-800 border-b last:border-b-0">{ind.name}</div>
                          ))
                        ) : (
                          <div className="px-4 py-3 text-sm text-gray-500 italic text-center">{availableIndustries.length === 0 ? "No industries found in database." : "No matching industries found."}</div>
                        )}
                      </div>
                    )}
                  </div>
                </div>
                <div className="col-span-2 p-4 border border-dashed border-gray-300 rounded bg-gray-50">
                  <div className="flex justify-between items-center mb-4">
                    <label className="text-[0.85rem] font-medium text-gray-700">Product Images</label>
                    <button type="button" onClick={() => imageInputRef.current.click()} className="px-3 py-1.5 text-xs font-medium text-blue-600 bg-blue-50 border border-blue-100 rounded hover:bg-blue-100 transition-colors">
                      + Browse Images
                    </button>
                    <input type="file" multiple accept="image/*" ref={imageInputRef} onChange={handleImageSelection} className="hidden" />
                  </div>
                  <div className="flex flex-wrap gap-4">
                    {formData.existingImages.map((url, index) => (
                      <div key={`exist-${index}`} className="relative w-24 h-24 border rounded bg-white shadow-sm group">
                        <img src={url} alt="existing" className="w-full h-full object-cover rounded" />
                        <button type="button" onClick={() => handleRemoveExistingImage(index)} className="absolute -top-2 -right-2 bg-red-50 text-red-500 border border-red-200 rounded-full p-1 opacity-0 group-hover:opacity-100 transition-opacity shadow-sm hover:bg-red-100">
                          <X className="w-3 h-3" />
                        </button>
                      </div>
                    ))}
                    {newImages.map((imgObj, index) => (
                      <div key={`new-${index}`} className="relative w-24 h-24 border border-blue-300 rounded bg-white shadow-sm group">
                        <img src={imgObj.previewUrl} alt="preview" className="w-full h-full object-cover rounded" />
                        <div className="absolute inset-0 bg-blue-500/5 rounded"></div>
                        <span className="absolute bottom-1 left-1 bg-blue-600 text-white text-[10px] px-1.5 py-0.5 rounded">NEW</span>
                        <button type="button" onClick={() => handleRemoveNewImage(index)} className="absolute -top-2 -right-2 bg-red-50 text-red-500 border border-red-200 rounded-full p-1 opacity-0 group-hover:opacity-100 transition-opacity shadow-sm hover:bg-red-100">
                          <X className="w-3 h-3" />
                        </button>
                      </div>
                    ))}
                    {formData.existingImages.length === 0 && newImages.length === 0 && (
                      <div className="w-full text-center py-6 text-sm text-gray-400 italic">No images selected. Click "Browse Images" to upload.</div>
                    )}
                  </div>
                </div>
                <div className="col-span-2 flex flex-col md:flex-row gap-6 border-t border-gray-100 pt-4">
                  <label className="flex items-center cursor-pointer group">
                    <input type="checkbox" id="featured" name="featured" checked={formData.featured} onChange={handleInputChange} className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-0 cursor-pointer" />
                    <span className="ml-2 text-[0.85rem] font-medium text-gray-700 group-hover:text-blue-600 transition-colors">Mark as Featured</span>
                  </label>
                  <label className="flex items-center cursor-pointer group">
                    <input type="checkbox" id="published" name="published" checked={formData.published} onChange={handleInputChange} className="w-4 h-4 text-green-600 border-gray-300 rounded focus:ring-0 cursor-pointer" />
                    <span className="ml-2 text-[0.85rem] font-medium text-gray-700 group-hover:text-green-600 transition-colors">Publish immediately</span>
                  </label>
                  <label className="flex items-center cursor-pointer group">
                    <input type="checkbox" id="manageStock" name="manageStock" checked={formData.manageStock} onChange={handleInputChange} className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-0 cursor-pointer" />
                    <span className="ml-2 text-[0.85rem] font-medium text-gray-700 group-hover:text-blue-600 transition-colors">Manage Stock</span>
                  </label>
                </div>
                {formData.manageStock && (
                  <div className="col-span-2 md:col-span-1">
                    <label className="block mb-1.5 text-[0.80rem] font-medium text-gray-600">Stock Quantity <span className="text-red-500">*</span></label>
                    <input type="number" name="stockQuantity" min="0" value={formData.stockQuantity} onChange={handleInputChange} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none" placeholder="e.g. 100" required={formData.manageStock} />
                  </div>
                )}
                <div className="col-span-2 md:col-span-1">
                  <label className="block mb-1.5 text-[0.80rem] font-medium text-gray-600">Expiry Date <span className="text-gray-400 font-normal">(Optional)</span></label>
                  <ExpiryDatePicker
                    value={formData.expiryDate}
                    onChange={(val) => setFormData({ ...formData, expiryDate: val })}
                    className="px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white"
                  />
                </div>
                <div className="col-span-2 border-t border-gray-100 pt-4">
                  <label className="flex items-center mb-3 cursor-pointer group">
                    <input type="checkbox" id="autoOfferOnExpiry" name="autoOfferOnExpiry" checked={formData.autoOfferOnExpiry} onChange={handleInputChange} className="w-4 h-4 text-orange-500 border-gray-300 rounded focus:ring-0 cursor-pointer" />
                    <span className="ml-2 text-[0.85rem] font-medium text-gray-700 group-hover:text-orange-600 transition-colors">Enable Automated Expiry Offer</span>
                  </label>
                  {formData.autoOfferOnExpiry && (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 bg-gray-50 p-4 rounded border border-gray-100">
                      <div>
                        <label className="block mb-1.5 text-[0.80rem] font-medium text-gray-600">Expiry Threshold (Days)</label>
                        <input type="number" name="expiryThresholdDays" min="1" value={formData.expiryThresholdDays} onChange={handleInputChange} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-orange-400 outline-none" placeholder="e.g. 7" />
                      </div>
                      <div>
                        <label className="block mb-1.5 text-[0.80rem] font-medium text-gray-600">Expiry Discount (%)</label>
                        <input type="number" name="expiryDiscountPercent" min="0" max="100" step="0.5" value={formData.expiryDiscountPercent} onChange={handleInputChange} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-orange-400 outline-none" placeholder="e.g. 10" />
                      </div>
                    </div>
                  )}
                </div>
              </div>
              <div className="mb-4 border-t border-gray-100 pt-4">
                <label className="flex items-center mb-3 cursor-pointer group">
                  <input type="checkbox" name="hasVariants" checked={formData.hasVariants} onChange={handleInputChange} className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-0 cursor-pointer" />
                  <span className="ml-2 text-[0.85rem] font-medium text-gray-700 group-hover:text-blue-600 transition-colors">Product has variants (e.g. colors, sizes)</span>
                </label>
                {formData.hasVariants && (
                  <div className="space-y-4 bg-gray-50 p-4 rounded border border-gray-100">
                    <div className="flex items-center justify-between">
                      <label className="text-[0.85rem] font-medium text-gray-700">Variants Configuration</label>
                      <button type="button" onClick={addVariantRow} className="text-[0.80rem] text-blue-600 hover:text-blue-700 font-medium transition-colors">+ Add Variant</button>
                    </div>
                    {formData.variants.map((variant, vIndex) => (
                      <div key={vIndex} className="p-4 bg-white border border-gray-200 rounded shadow-sm relative">
                        <button type="button" onClick={() => removeVariantRow(vIndex)} className="absolute top-2 right-2 text-red-400 hover:text-red-600"><X className="w-4 h-4" /></button>
                        <div className="grid grid-cols-2 gap-4 mb-3">
                          <div>
                            <label className="block text-[0.75rem] text-gray-500 mb-1">SKU</label>
                            <input type="text" value={variant.sku} onChange={(e) => handleVariantChange(vIndex, 'sku', e.target.value)} className="w-full px-2 py-1 text-[0.85rem] border border-gray-200 rounded focus:border-blue-400 outline-none" />
                          </div>
                          <div>
                            <label className="block text-[0.75rem] text-gray-500 mb-1">Stock</label>
                            <input type="number" min="0" value={variant.stockQuantity} onChange={(e) => handleVariantChange(vIndex, 'stockQuantity', e.target.value)} className="w-full px-2 py-1 text-[0.85rem] border border-gray-200 rounded focus:border-blue-400 outline-none" />
                          </div>
                          <div>
                            <label className="block text-[0.75rem] text-gray-500 mb-1">MRP</label>
                            <input type="number" min="0" step="0.01" value={variant.mrp} onChange={(e) => handleVariantChange(vIndex, 'mrp', e.target.value)} className="w-full px-2 py-1 text-[0.85rem] border border-gray-200 rounded focus:border-blue-400 outline-none" />
                          </div>
                          <div>
                            <label className="block text-[0.75rem] text-gray-500 mb-1">Selling Price</label>
                            <input type="number" min="0" step="0.01" value={variant.sellingPrice} onChange={(e) => handleVariantChange(vIndex, 'sellingPrice', e.target.value)} className="w-full px-2 py-1 text-[0.85rem] border border-gray-200 rounded focus:border-blue-400 outline-none" />
                          </div>
                        </div>
                        <div>
                          <div className="flex items-center justify-between mb-2">
                            <label className="text-[0.75rem] font-medium text-gray-600">Attributes (e.g. Size: L, Color: Red)</label>
                            <button type="button" onClick={() => addVariantAttr(vIndex)} className="text-[0.75rem] text-blue-600">+ Add Attr</button>
                          </div>
                          <div className="space-y-2">
                            {variant.attributes && variant.attributes.map((attr, aIndex) => (
                              <div key={aIndex} className="flex gap-2">
                                <input type="text" placeholder="Key (e.g. Color)" value={attr.key} onChange={(e) => handleVariantAttrChange(vIndex, aIndex, 'key', e.target.value)} className="flex-1 px-2 py-1 text-[0.80rem] border border-gray-200 rounded focus:border-blue-400 outline-none" />
                                <input type="text" placeholder="Value (e.g. Red)" value={attr.value} onChange={(e) => handleVariantAttrChange(vIndex, aIndex, 'value', e.target.value)} className="flex-1 px-2 py-1 text-[0.80rem] border border-gray-200 rounded focus:border-blue-400 outline-none" />
                                <button type="button" onClick={() => removeVariantAttr(vIndex, aIndex)} className="text-red-400 hover:text-red-600"><X className="w-3 h-3" /></button>
                              </div>
                            ))}
                          </div>
                        </div>
                      </div>
                    ))}
                    {formData.variants.length === 0 && (
                      <p className="text-[0.80rem] text-gray-500 italic">No variants added yet.</p>
                    )}
                  </div>
                )}
              </div>
              <div className="mb-4 border-t border-gray-100 pt-4">
                <div className="flex items-center justify-between mb-2">
                  <label className="text-[0.85rem] font-medium text-gray-700">Specifications</label>
                  <button type="button" onClick={addSpecRow} className="text-[0.80rem] text-blue-600 hover:text-blue-700 font-medium transition-colors">+ Add Spec</button>
                </div>
                <div className="space-y-2">
                  {formData.specs.map((spec, index) => (
                    <div key={index} className="flex gap-2">
                      <input type="text" placeholder="Key (e.g., Weight)" value={spec.key} onChange={(e) => handleSpecChange(index, 'key', e.target.value)} className="flex-1 px-3 py-1.5 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none" />
                      <input type="text" placeholder="Value (e.g., 50kg)" value={spec.value} onChange={(e) => handleSpecChange(index, 'value', e.target.value)} className="flex-1 px-3 py-1.5 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none" />
                      {formData.specs.length > 1 && (
                        <button type="button" onClick={() => removeSpecRow(index)} className="px-3 text-red-500 hover:text-red-600 bg-red-50 hover:bg-red-100 border border-red-200 rounded transition-colors"><X className="w-4 h-4" /></button>
                      )}
                    </div>
                  ))}
                </div>
              </div>
              <div className="flex justify-end pt-4 space-x-3 border-t border-gray-100 mt-6">
                <button type="button" onClick={handleCloseModal} className="px-4 py-2 text-[0.85rem] text-gray-600 bg-white border border-gray-200 rounded hover:bg-gray-50 font-medium transition-colors">Cancel</button>
                <button type="submit" disabled={isSubmitting} className="px-4 py-2 text-[0.85rem] text-white bg-blue-600 rounded hover:bg-blue-700 font-medium disabled:opacity-50 transition-colors shadow-sm">{isSubmitting ? 'Saving...' : (editingProduct ? 'Update Product' : 'Create Product')}</button>
              </div>
            </form>
          </div>
        </div>
      )}
      {uploadReport && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-60 backdrop-blur-sm p-4">
          <div className="w-full max-w-2xl bg-white rounded-lg shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
            {/* Header */}
            <div className="p-6 border-b bg-gray-50 flex justify-between items-center">
              <div className="flex items-center gap-2">
                <UploadCloud className="w-6 h-6 text-[#1E3A5F]" />
                <h2 className="text-xl font-bold text-[#1E3A5F]">Bulk Upload Results</h2>
              </div>
              <button onClick={() => setUploadReport(null)} className="text-gray-500 hover:text-red-500 text-2xl leading-none">&times;</button>
            </div>

            {/* Content */}
            <div className="p-6 overflow-y-auto flex-1 space-y-6">
              {/* Summary Cards */}
              <div className="grid grid-cols-2 gap-4">
                <div className="bg-green-50 border border-green-200 p-4 rounded-lg flex items-center gap-4">
                  <div className="p-3 bg-green-500 rounded-full text-white">
                    <CheckCircle className="w-6 h-6" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-green-800">Successful</p>
                    <p className="text-2xl font-extrabold text-green-900">{uploadReport.successfulUploads}</p>
                  </div>
                </div>

                <div className={`${uploadReport.failedUploads > 0 ? 'bg-red-50 border-red-200' : 'bg-gray-50 border-gray-200'} border p-4 rounded-lg flex items-center gap-4`}>
                  <div className={`p-3 rounded-full text-white ${uploadReport.failedUploads > 0 ? 'bg-red-500' : 'bg-gray-400'}`}>
                    <AlertCircle className="w-6 h-6" />
                  </div>
                  <div>
                    <p className={`text-sm font-semibold ${uploadReport.failedUploads > 0 ? 'text-red-800' : 'text-gray-700'}`}>Failed</p>
                    <p className={`text-2xl font-extrabold ${uploadReport.failedUploads > 0 ? 'text-red-900' : 'text-gray-950'}`}>{uploadReport.failedUploads}</p>
                  </div>
                </div>
              </div>

              {/* Detailed Error List */}
              {uploadReport.errors && uploadReport.errors.length > 0 ? (
                <div className="space-y-3">
                  <div className="flex items-center gap-2 text-red-700">
                    <AlertTriangle className="w-5 h-5" />
                    <h3 className="font-bold text-base">Detailed Failure Log</h3>
                  </div>
                  <div className="border border-red-100 rounded-lg overflow-hidden bg-red-50/30">
                    <div className="max-h-60 overflow-y-auto divide-y divide-red-50 text-sm">
                      {uploadReport.errors.map((err, index) => (
                        <div key={index} className="p-3 flex items-start gap-3 hover:bg-red-50/60 transition-colors">
                          <span className="font-mono text-xs font-bold text-red-600 bg-red-100 border border-red-200 px-1.5 py-0.5 rounded shrink-0">
                            Error
                          </span>
                          <p className="font-medium text-gray-700">{err}</p>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              ) : (
                <div className="bg-blue-50 border border-blue-100 p-4 rounded-lg flex items-center gap-3 text-blue-800 text-sm font-medium">
                  <CheckCircle className="w-5 h-5 text-blue-500 shrink-0" />
                  <p>Excellent! All products in the CSV file have been successfully validated and imported without any issues.</p>
                </div>
              )}
            </div>

            {/* Footer */}
            <div className="p-4 border-t bg-gray-50 flex justify-end">
              <button onClick={() => setUploadReport(null)} className="px-6 py-2 text-white bg-[#1E3A5F] rounded hover:bg-blue-900 font-medium shadow-sm transition-colors">
                Dismiss
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
function ProductImageCarousel({ images, productName }) {
const [currentIndex, setCurrentIndex] = useState(0);
if (!images || images.length === 0) {
return (
<div className="w-24 h-24 bg-gray-100 border border-gray-200 rounded flex items-center justify-center text-xs text-gray-400">
        No Image
      </div>
    );
  }
if (images.length === 1) {
return <img src={images[0]} alt={productName} className="w-24 h-24 object-cover rounded shadow-sm border border-gray-200" />;
  }
const nextImage = (e) => {
e.stopPropagation();
setCurrentIndex((prev) => (prev + 1) % images.length);
  };
const prevImage = (e) => {
e.stopPropagation();
setCurrentIndex((prev) => (prev === 0 ? images.length - 1 : prev - 1));
  };
let touchStartX = 0;
let touchEndX = 0;
const handleTouchStart = (e) => { touchStartX = e.targetTouches[0].clientX; };
const handleTouchMove = (e) => { touchEndX = e.targetTouches[0].clientX; };
const handleTouchEnd = () => {
if (touchStartX - touchEndX > 40) nextImage({ stopPropagation: () => { } });
if (touchStartX - touchEndX < -40) prevImage({ stopPropagation: () => { } });
  };
return (
<div 
className = "relative w-24 h-24 group cursor-pointer overflow-hidden rounded shadow-sm border border-gray-200"
onTouchStart = { handleTouchStart }
onTouchMove = { handleTouchMove }
onTouchEnd = { handleTouchEnd }
    >
<img 
src = { images[currentIndex]}
alt = {`${productName} - ${currentIndex + 1}`
}
className = "w-full h-full object-cover transition-opacity duration-300"
      />
<button 
onClick = { prevImage }
className = "absolute left-0 top-1/2 -translate-y-1/2 bg-black/60 text-white px-1.5 py-2 opacity-0 group-hover:opacity-100 transition-opacity hover:bg-black/80"
      >
        &#10094;
      </button >
<button 
onClick = { nextImage }
className = "absolute right-0 top-1/2 -translate-y-1/2 bg-black/60 text-white px-1.5 py-2 opacity-0 group-hover:opacity-100 transition-opacity hover:bg-black/80"
      >
        &#10095;
      </button >
<div className="absolute bottom-1 left-1/2 -translate-x-1/2 flex gap-1 bg-black/30 px-1.5 py-0.5 rounded-full">
        {images.map((_, idx) => (
          <div 
            key={idx}
            className={`w-1.5 h-1.5 rounded-full transition-all ${idx === currentIndex ? 'bg-white scale-110' : 'bg-white/50'}`} 
          />
        ))}
      </div>
    </div >
  );
}

function InventoryRow({ product, onSaveSuccess }) {
  const [mrp, setMrp] = useState(product.mrp || '');
  const [sellingPrice, setSellingPrice] = useState(product.sellingPrice || '');
  const [manageStock, setManageStock] = useState(product.manageStock || false);
  const [stockQuantity, setStockQuantity] = useState(product.stockQuantity || 0);
  const [expiryDate, setExpiryDate] = useState(product.expiryDate || '');
  const [saving, setSaving] = useState(false);

  // Synchronize state with product changes if necessary (e.g. after a global refresh)
  useEffect(() => {
    setMrp(product.mrp || '');
    setSellingPrice(product.sellingPrice || '');
    setManageStock(product.manageStock || false);
    setStockQuantity(product.stockQuantity || 0);
    setExpiryDate(product.expiryDate || '');
  }, [product]);

  const handleSave = async () => {
    if (mrp && sellingPrice && Number(sellingPrice) > Number(mrp)) {
      alert('Selling price cannot be greater than MRP.');
      return;
    }
    setSaving(true);
    try {
      await api.patch(`/api/products/${product.id || product._id}/stock-and-price`, {
        mrp: mrp === '' ? null : Number(mrp),
        sellingPrice: sellingPrice === '' ? null : Number(sellingPrice),
        manageStock,
        stockQuantity: Number(stockQuantity),
        expiryDate: expiryDate || null
      });
      onSaveSuccess();
    } catch (err) {
      alert(err.response?.data?.message || "Failed to update stock and price.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <tr className="hover:bg-gray-50 transition-colors">
      <td className="p-4 border-b border-gray-100">
        <ProductImageCarousel images={product.imageUrls} productName={product.name} />
      </td>
      <td className="p-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-700">
        {product.productCode ? product.productCode : <span className="text-gray-400">-</span>}
      </td>
      <td className="p-4 font-medium border-b border-gray-100 text-[0.85rem] text-gray-800">
        <div className="flex flex-wrap items-center gap-2">
          <span>{product.name}</span>
          {(product.stockQuantity !== null && product.stockQuantity !== undefined ? Number(product.stockQuantity) : 0) < 20 && (
            <span className="px-1.5 py-0.5 text-[0.7rem] font-medium text-red-600 border border-red-200 rounded">
              Low Stock ({product.stockQuantity || 0})
            </span>
          )}
        </div>
      </td>
      <td className="p-4 border-b border-gray-100">
        <input
          type="number"
          value={mrp}
          onChange={(e) => setMrp(e.target.value)}
          placeholder="MRP"
          className="w-full px-3 py-1.5 border border-gray-200 rounded focus:ring-0 focus:border-blue-400 text-[0.85rem] outline-none"
          min="0"
          step="0.01"
        />
      </td>
      <td className="p-4 border-b border-gray-100">
        <input
          type="number"
          value={sellingPrice}
          onChange={(e) => setSellingPrice(e.target.value)}
          placeholder="Selling Price"
          className="w-full px-3 py-1.5 border border-gray-200 rounded focus:ring-0 focus:border-blue-400 text-[0.85rem] outline-none"
          min="0"
          step="0.01"
        />
      </td>
      <td className="p-4 border-b border-gray-100">
        <label className="relative inline-flex items-center cursor-pointer">
          <input
            type="checkbox"
            className="sr-only peer"
            checked={manageStock}
            onChange={(e) => setManageStock(e.target.checked)}
          />
          <div className="w-8 h-4 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-3 after:w-3 after:transition-all peer-checked:bg-[#2563eb]"></div>
        </label>
      </td>
      <td className="p-4 border-b border-gray-100">
        <input
          type="number"
          value={stockQuantity}
          onChange={(e) => setStockQuantity(e.target.value)}
          disabled={!manageStock}
          placeholder="Stock"
          className={`w-full px-3 py-1.5 border border-gray-200 rounded focus:ring-0 focus:border-blue-400 text-[0.85rem] outline-none ${
            !manageStock ? 'bg-gray-50 text-gray-400 cursor-not-allowed' : 'bg-white'
          }`}
          min="0"
        />
      </td>
      <td className="p-4 border-b border-gray-100">
        <ExpiryDatePicker
          value={expiryDate}
          onChange={setExpiryDate}
          className="px-3 py-1.5 border border-gray-200 rounded focus:ring-0 focus:border-blue-400 text-[0.85rem] outline-none bg-white"
        />
      </td>
      <td className="p-4 border-b border-gray-100">
        <button
          onClick={handleSave}
          disabled={saving}
          className={`px-4 py-1.5 rounded text-[0.85rem] font-medium transition-colors shadow-sm ${
            saving ? 'bg-gray-100 text-gray-400 border border-gray-200' : 'bg-green-600 text-white hover:bg-green-700'
          }`}
        >
          {saving ? 'Saving...' : 'Save'}
        </button>
      </td>
    </tr>
  );
}

function ExpiryOfferRow({ product, onSaveSuccess, onToggleAutoOffer }) {
  const [autoOfferOnExpiry, setAutoOfferOnExpiry] = useState(product.autoOfferOnExpiry || false);
  const [expiryThresholdDays, setExpiryThresholdDays] = useState(product.expiryThresholdDays || 7);
  const [expiryDiscountPercent, setExpiryDiscountPercent] = useState(product.expiryDiscountPercent || 10.0);
  const [expiryDate, setExpiryDate] = useState(product.expiryDate || '');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setAutoOfferOnExpiry(product.autoOfferOnExpiry || false);
    setExpiryThresholdDays(product.expiryThresholdDays || 7);
    setExpiryDiscountPercent(product.expiryDiscountPercent || 10.0);
    setExpiryDate(product.expiryDate || '');
  }, [product]);

  const handleSave = async () => {
    setSaving(true);
    try {
      await api.patch(`/api/products/${product.id || product._id}/stock-and-price`, {
        autoOfferOnExpiry,
        expiryThresholdDays: Number(expiryThresholdDays),
        expiryDiscountPercent: Number(expiryDiscountPercent),
        expiryDate: expiryDate || null
      });
      onSaveSuccess();
    } catch (err) {
      alert(err.response?.data?.message || "Failed to update expiry offer settings.");
    } finally {
      setSaving(false);
    }
  };

  const getStatus = () => {
    if (!autoOfferOnExpiry) return { label: 'Disabled', color: 'bg-gray-100 text-gray-500 border-gray-200' };
    if (product.isOnExpiryOffer) return { label: 'Active Offer', color: 'bg-green-100 text-green-700 border-green-300' };
    if (!expiryDate) return { label: 'No Expiry Date', color: 'bg-yellow-100 text-yellow-700 border-yellow-300' };
    return { label: 'Pending Window', color: 'bg-blue-100 text-blue-700 border-blue-300' };
  };

  const status = getStatus();

  return (
    <tr className="hover:bg-gray-50 transition-colors">
      <td className="p-4 border-b border-gray-100">
        <ProductImageCarousel images={product.imageUrls} productName={product.name} />
      </td>
      <td className="p-4 border-b border-gray-100 text-[0.85rem] font-medium text-gray-700">
        {product.productCode ? product.productCode : <span className="text-gray-400">-</span>}
      </td>
      <td className="p-4 font-medium border-b border-gray-100 text-[0.85rem] text-gray-800">
        {product.name}
      </td>
      <td className="p-4 border-b border-gray-100">
        <ExpiryDatePicker
          value={expiryDate}
          onChange={setExpiryDate}
          className="px-3 py-1.5 border border-gray-200 rounded focus:ring-0 focus:border-[#E8A020] text-[0.85rem] outline-none bg-white"
        />
      </td>
      <td className="p-4 border-b border-gray-100 text-center">
        <label className="relative inline-flex items-center cursor-pointer">
          <input
            type="checkbox"
            className="sr-only peer"
            checked={autoOfferOnExpiry}
            onChange={(e) => {
              setAutoOfferOnExpiry(e.target.checked);
              onToggleAutoOffer();
            }}
          />
          <div className="w-8 h-4 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-3 after:w-3 after:transition-all peer-checked:bg-[#E8A020]"></div>
        </label>
      </td>
      <td className="p-4 border-b border-gray-100">
        <input
          type="number"
          value={expiryThresholdDays}
          onChange={(e) => setExpiryThresholdDays(e.target.value)}
          disabled={!autoOfferOnExpiry}
          className={`w-full px-3 py-1.5 border border-gray-200 rounded text-center focus:ring-0 focus:border-[#E8A020] text-[0.85rem] outline-none ${
            !autoOfferOnExpiry ? 'bg-gray-50 text-gray-400 cursor-not-allowed' : 'bg-white'
          }`}
          min="1"
        />
      </td>
      <td className="p-4 border-b border-gray-100">
        <input
          type="number"
          value={expiryDiscountPercent}
          onChange={(e) => setExpiryDiscountPercent(e.target.value)}
          disabled={!autoOfferOnExpiry}
          className={`w-full px-3 py-1.5 border border-gray-200 rounded text-center focus:ring-0 focus:border-[#E8A020] text-[0.85rem] outline-none ${
            !autoOfferOnExpiry ? 'bg-gray-50 text-gray-400 cursor-not-allowed' : 'bg-white'
          }`}
          min="0"
          max="100"
          step="0.5"
        />
      </td>
      <td className="p-4 border-b border-gray-100">
        <span className={`px-2 py-0.5 text-[0.7rem] font-medium border rounded ${status.color}`}>
          {status.label}
        </span>
      </td>
      <td className="p-4 border-b border-gray-100">
        <button
          onClick={handleSave}
          disabled={saving}
          className={`px-4 py-1.5 rounded text-[0.85rem] font-medium transition-colors shadow-sm ${
            saving ? 'bg-gray-100 text-gray-400 border border-gray-200' : 'bg-green-600 text-white hover:bg-green-700'
          }`}
        >
          {saving ? 'Saving...' : 'Save'}
        </button>
      </td>
    </tr>
  );
}
