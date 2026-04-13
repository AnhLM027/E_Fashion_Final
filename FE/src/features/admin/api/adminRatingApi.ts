import axiosClient from "@/lib/axiosClient";
import { API_ENDPOINTS } from "@/config/api.config";

export const adminRatingApi = {
  getAllRatings: () => axiosClient.get(API_ENDPOINTS.STAFF.RATINGS),
  deleteRating: (id: string) => axiosClient.delete(API_ENDPOINTS.STAFF.RATING_ID(id)),
};
