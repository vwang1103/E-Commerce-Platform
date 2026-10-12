import Image from "next/image";

const SEEDED_PRODUCT_IMAGES: Readonly<Record<number, string>> = {
  1001: "/store-assets/product_img1.png",
  1002: "/store-assets/product_img2.png",
  1003: "/store-assets/product_img3.png",
  1004: "/store-assets/product_img4.png",
  1005: "/store-assets/product_img5.png",
  1006: "/store-assets/product_img6.png",
  1007: "/store-assets/product_img7.png",
  1008: "/store-assets/product_img8.png",
  1009: "/store-assets/skyline-puzzle.png",
  1010: "/store-assets/product_img9.png",
  1011: "/store-assets/product_img11.png",
  1012: "/store-assets/product_img12.png",
  1013: "/store-assets/product_img13.png",
  1014: "/store-assets/product_img14.png",
  1015: "/store-assets/product_img15.png",
  1016: "/store-assets/product_img16.png",
  1017: "/store-assets/product_img10.png",
  1018: "/store-assets/planner-set.png",
  1019: "/store-assets/book-stack.png",
  1020: "/store-assets/yoga-kit.png",
  1021: "/store-assets/hiking-daypack.png",
  1022: "/store-assets/electric-toothbrush.png",
  1023: "/store-assets/skincare-set.png",
};

const CATEGORY_IMAGES: Readonly<Record<number, string>> = {
  100: "/store-assets/product_img4.png",
  200: "/store-assets/product_img1.png",
  300: "/store-assets/planner-set.png",
  400: "/store-assets/yoga-kit.png",
  500: "/store-assets/skincare-set.png",
  600: "/store-assets/product_img14.png",
};

/** Shows product photography for seeded products and a category photo for newly created products. */
export function ProductArtwork({
  productId,
  name,
  categoryId,
  className = "aspect-square",
}: Readonly<{ productId?: number; name: string; categoryId?: number; className?: string }>) {
  const image = SEEDED_PRODUCT_IMAGES[productId ?? 0] ?? CATEGORY_IMAGES[categoryId ?? 0] ?? "/store-assets/product_img16.png";

  return (
    <div className={`group relative isolate overflow-hidden bg-[#f5f5f3] ${className}`}>
      <Image
        src={image}
        alt={name}
        fill
        sizes="(max-width: 640px) 50vw, (max-width: 1024px) 33vw, 25vw"
        className="object-contain p-[12%] transition-transform duration-300 group-hover:scale-105"
      />
    </div>
  );
}
