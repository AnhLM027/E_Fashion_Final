import { useEffect, useState } from "react";
import { adminRatingApi } from "@/features/admin/api/adminRatingApi";
import { Star, Trash2, ExternalLink } from "lucide-react";

const AdminReviewListPage = () => {
  const [reviews, setReviews] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchReviews();
  }, []);

  const fetchReviews = async () => {
    try {
      setLoading(true);
      const res = await adminRatingApi.getAllRatings();
      setReviews(res || []);
    } catch (error) {
      console.error("Failed to fetch reviews:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm("Are you sure you want to delete this review?")) return;
    try {
      await adminRatingApi.deleteRating(id);
      setReviews(reviews.filter((r) => r.id !== id));
    } catch (error) {
      console.error("Failed to delete review:", error);
    }
  };

  if (loading) return <div className="p-8 text-center text-gray-500">Loading reviews...</div>;

  return (
    <div className="p-8 space-y-6 max-w-7xl mx-auto">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-800">Reviews Management</h1>
        <div className="text-sm text-gray-500">Total: {reviews.length}</div>
      </div>

      <div className="bg-white rounded-3xl shadow-sm overflow-hidden border border-gray-100">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100 text-sm text-gray-600">
                <th className="px-6 py-4 font-semibold">Product</th>
                <th className="px-6 py-4 font-semibold">Customer</th>
                <th className="px-6 py-4 font-semibold">Rating</th>
                <th className="px-6 py-4 font-semibold">Review</th>
                <th className="px-6 py-4 font-semibold">Date</th>
                <th className="px-6 py-4 font-semibold text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {reviews.map((review: any) => (
                <tr key={review.id} className="hover:bg-gray-50/50 transition-colors">
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <img
                        src={review.orderItem?.imageUrl}
                        alt=""
                        className="w-10 h-10 rounded-lg object-cover bg-gray-100"
                      />
                      <div className="max-w-[200px]">
                        <div className="font-medium text-gray-800 truncate">{review.orderItem?.productName}</div>
                        <div className="text-xs text-gray-500">
                          {review.orderItem?.colorName} / {review.orderItem?.sizeName}
                        </div>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="text-sm font-medium text-gray-800">{review.userName}</div>
                    <div className="text-xs text-gray-500 text-truncate max-w-[120px]">ID: {review.userId}</div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-0.5">
                      {[1, 2, 3, 4, 5].map((s) => (
                        <Star
                          key={s}
                          size={14}
                          className={s <= review.rating ? "fill-yellow-400 text-yellow-400" : "text-gray-200"}
                        />
                      ))}
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="text-sm text-gray-600 max-w-[300px] line-clamp-2">
                      {review.reviewText || <span className="text-gray-300 italic">No comment</span>}
                    </div>
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-500 whitespace-nowrap">
                    {new Date(review.createdAt).toLocaleDateString()}
                  </td>
                  <td className="px-6 py-4 text-right">
                    <div className="flex justify-end gap-2">
                      <button
                        onClick={() => handleDelete(review.id)}
                        className="p-2 text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                        title="Delete review"
                      >
                        <Trash2 size={18} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {reviews.length === 0 && (
            <div className="p-12 text-center text-gray-400">No reviews found</div>
          )}
        </div>
      </div>
    </div>
  );
};

export default AdminReviewListPage;
