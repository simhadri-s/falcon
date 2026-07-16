import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Package, CheckCircle, XCircle, Tag, List } from 'lucide-react';
import api from '../services/api';

export default function AdminProductDetail() {
  const { slug } = useParams();
  const navigate = useNavigate();
  
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchProductDetails = async () => {
      try {
        const response = await api.get(`/api/products/${slug}`);
        setProduct(response.data.data || response.data);
      } catch (err) {
        console.error("Error fetching product:", err);
        setError('Failed to load product details.');
      } finally {
        setLoading(false);
      }
    };

    fetchProductDetails();
  }, [slug]);

  if (loading) return <div className="p-8 text-center text-gray-500 font-bold text-lg mt-20">Loading Product Details...</div>;
  if (error) return <div className="p-8 text-center text-red-500 font-bold text-lg mt-20">{error}</div>;
  if (!product) return <div className="p-8 text-center text-gray-500 mt-20">Product not found.</div>;

  const formatPrice = (value) => {
    const amount = Number(value);
    return Number.isFinite(amount) ? `₹${amount.toLocaleString('en-IN')}` : '';
  };

  const hasSellingPrice = product.sellingPrice !== null && product.sellingPrice !== undefined && product.sellingPrice !== '' && Number(product.sellingPrice) > 0;
  const hasMrp = product.mrp !== null && product.mrp !== undefined && product.mrp !== '';
  const displayPrice = hasSellingPrice ? product.sellingPrice : product.mrp;

  return (
    <div className="max-w-5xl mx-auto pb-10">
      {/* Back Button */}
      <button 
        onClick={() => navigate(-1)}
        className="mb-6 text-[#1E3A5F] hover:underline flex items-center gap-2 font-medium"
      >
        <ArrowLeft className="w-5 h-5" /> Back
      </button>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        
        {/* Header Area */}
        <div className="p-6 border-b bg-gray-50 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-3 mb-2">
              <h1 className="text-3xl font-black text-[#1E3A5F]">{product.name}</h1>
              {product.featured && (
                <span className="px-3 py-1 text-xs font-bold bg-yellow-100 text-yellow-800 rounded-full border border-yellow-200">
                  Featured
                </span>
              )}
            </div>
            <p className="text-gray-500 font-mono flex items-center gap-2">
              <Tag className="w-4 h-4" /> Code: {product.productCode || 'N/A'}
            </p>
            {displayPrice !== null && displayPrice !== undefined && displayPrice !== '' && (
              <div className="mt-4 flex items-end gap-3">
                <span className="text-3xl font-black text-[#1E3A5F]">
                  {formatPrice(Number(displayPrice))}
                </span>
                {hasSellingPrice && hasMrp && Number(product.mrp) > Number(product.sellingPrice) && (
                  <span className="text-sm font-medium text-gray-400 line-through pb-1">
                    {formatPrice(Number(product.mrp))}
                  </span>
                )}
              </div>
            )}
          </div>
          
          <div className="flex items-center gap-3">
            <span className={`flex items-center gap-1 px-4 py-2 rounded-lg text-sm font-bold border ${product.published ? 'bg-green-50 text-green-700 border-green-200' : 'bg-gray-100 text-gray-600 border-gray-300'}`}>
              {product.published ? <CheckCircle className="w-4 h-4" /> : <XCircle className="w-4 h-4" />}
              {product.published ? 'Published' : 'Draft'}
            </span>
          </div>
        </div>
        
        {/* Main Content Area */}
        <div className="p-6 grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          {/* Images Gallery */}
          <div className="lg:col-span-1 space-y-4">
            {product.imageUrls && product.imageUrls.length > 0 ? (
              <div className="space-y-4">
                <img src={product.imageUrls[0]} alt={product.name} className="w-full h-auto object-cover rounded-xl border border-gray-200 shadow-sm" />
                
                {product.imageUrls.length > 1 && (
                  <div className="grid grid-cols-4 gap-2">
                    {product.imageUrls.slice(1).map((img, idx) => (
                      <img key={idx} src={img} alt={`Thumbnail ${idx}`} className="w-full h-16 object-cover rounded border border-gray-200" />
                    ))}
                  </div>
                )}
              </div>
            ) : (
              <div className="w-full h-64 bg-gray-100 rounded-xl flex items-center justify-center border border-gray-200">
                <p className="text-gray-400 font-bold">No Image Available</p>
              </div>
            )}
          </div>
          
          {/* Details & Specs */}
          <div className="lg:col-span-2 space-y-8">
            
            <section>
              <h2 className="text-xl font-bold text-gray-800 mb-3 flex items-center gap-2 border-b pb-2">
                <List className="w-5 h-5 text-[#E8A020]" /> Basic Information
              </h2>
              <div className="grid grid-cols-2 gap-4">
                <div className="p-4 bg-gray-50 rounded-lg border border-gray-100">
                  <p className="text-xs font-bold text-gray-400 uppercase mb-1">Category</p>
                  <p className="font-bold text-[#1E3A5F]">{product.category || 'Uncategorized'}</p>
                </div>
                <div className="p-4 bg-gray-50 rounded-lg border border-gray-100">
                  <p className="text-xs font-bold text-gray-400 uppercase mb-1">Industries</p>
                  <p className="font-bold text-[#1E3A5F]">
                    {product.industries && product.industries.length > 0 
                      ? product.industries.map((industry) => typeof industry === 'string' ? industry : industry.name).join(', ') 
                      : 'None'}
                  </p>
                </div>
              </div>
            </section>

            <section>
              <h2 className="text-xl font-bold text-gray-800 mb-3 flex items-center gap-2 border-b pb-2">
                <Package className="w-5 h-5 text-[#E8A020]" /> Description
              </h2>
              <div className="bg-gray-50 p-4 rounded-lg border border-gray-100">
                <p className="whitespace-pre-wrap text-gray-700 leading-relaxed">
                  {product.description || <span className="text-gray-400 italic">No description provided.</span>}
                </p>
              </div>
            </section>

            {product.specs && product.specs.length > 0 && (
              <section>
                <h2 className="text-xl font-bold text-gray-800 mb-3 flex items-center gap-2 border-b pb-2">
                  <Package className="w-5 h-5 text-[#E8A020]" /> Specifications
                </h2>
                <div className="bg-white border border-gray-200 rounded-lg overflow-hidden shadow-sm">
                  <table className="w-full text-left border-collapse">
                    <tbody className="divide-y divide-gray-100">
                      {product.specs.map((spec, index) => (
                        <tr key={index} className="hover:bg-gray-50 transition-colors">
                          <th className="p-3 w-1/3 bg-gray-50 text-gray-600 font-bold border-r border-gray-100">{spec.key}</th>
                          <td className="p-3 text-gray-800">{spec.value}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>
            )}

          </div>
        </div>

      </div>
    </div>
  );
}
