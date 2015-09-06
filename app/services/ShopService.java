package services;

import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductVariant;
import pactas.models.PactasContract;
import pactas.models.PactasCustomer;
import play.mvc.Http;

import java.util.Optional;

public interface ShopService {

    Cart getOrCreateCart(Http.Session session);

    Cart clearCart(final Cart cart);

    void setProductToCart(final Cart cart, final ProductProjection product, final ProductVariant variant, final int frequency);

    int getFrequency(final String cartId);

    Optional<ProductVariant> getSelectedVariant(final Cart cart);

    Optional<ProductVariant> variantFromId(final ProductProjection product, final int variantId);

    Cart createCartWithPactasInfo(final ProductProjection product, final PactasContract contract, final PactasCustomer customer);

    Optional<ProductProjection> getProduct();

    Order createOrder(Cart cart);
}
