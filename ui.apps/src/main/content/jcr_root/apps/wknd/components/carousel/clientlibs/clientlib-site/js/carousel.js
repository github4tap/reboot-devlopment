/**
 * WKND High-Performance Carousel Component
 * Accessible, touch-enabled, WCAG 2.1 compliant carousel controller.
 */
(function () {
    "use strict";

    class Carousel {
        constructor(element) {
            this.element = element;
            this.slides = Array.from(element.querySelectorAll(".cmp-carousel__item"));
            this.indicators = Array.from(element.querySelectorAll(".cmp-carousel__indicator"));
            this.prevBtn = element.querySelector(".cmp-carousel__action--previous");
            this.nextBtn = element.querySelector(".cmp-carousel__action--next");
            this.pauseBtn = element.querySelector(".cmp-carousel__action--toggle-pause");

            this.autoplay = element.dataset.cmpAutoplay === "true";
            this.delay = parseInt(element.dataset.cmpDelay, 10) || 5000;
            this.currentIndex = 0;
            this.isPlaying = this.autoplay;
            this.timer = null;

            // Touch gesture coordinates
            this.touchStartX = 0;
            this.touchEndX = 0;

            if (this.slides.length <= 1) {
                return;
            }

            this.init();
        }

        init() {
            // Check user accessibility preference for reduced motion
            const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
            if (prefersReducedMotion) {
                this.isPlaying = false;
            }

            this.bindEvents();

            if (this.isPlaying) {
                this.startAutoplay();
            }
        }

        bindEvents() {
            if (this.prevBtn) {
                this.prevBtn.addEventListener("click", () => {
                    this.pauseAutoplay();
                    this.prevSlide();
                });
            }

            if (this.nextBtn) {
                this.nextBtn.addEventListener("click", () => {
                    this.pauseAutoplay();
                    this.nextSlide();
                });
            }

            if (this.pauseBtn) {
                this.pauseBtn.addEventListener("click", () => {
                    if (this.isPlaying) {
                        this.pauseAutoplay();
                        this.updatePauseBtnUI(false);
                    } else {
                        this.startAutoplay();
                        this.updatePauseBtnUI(true);
                    }
                });
            }

            // Indicator clicks
            this.indicators.forEach((indicator, index) => {
                indicator.addEventListener("click", () => {
                    this.pauseAutoplay();
                    this.goToSlide(index);
                });
            });

            // Keyboard navigation (WCAG 2.1)
            this.element.addEventListener("keydown", (e) => {
                if (e.key === "ArrowLeft") {
                    e.preventDefault();
                    this.prevSlide();
                } else if (e.key === "ArrowRight") {
                    e.preventDefault();
                    this.nextSlide();
                }
            });

            // Pause on hover & focus for accessibility
            this.element.addEventListener("mouseenter", () => {
                if (this.isPlaying) {
                    this.clearTimer();
                }
            });

            this.element.addEventListener("mouseleave", () => {
                if (this.isPlaying) {
                    this.startAutoplay();
                }
            });

            this.element.addEventListener("focusin", () => {
                if (this.isPlaying) {
                    this.clearTimer();
                }
            });

            this.element.addEventListener("focusout", () => {
                if (this.isPlaying) {
                    this.startAutoplay();
                }
            });

            // Touch swipe gesture support for mobile
            this.element.addEventListener("touchstart", (e) => {
                this.touchStartX = e.changedTouches[0].screenX;
            }, { passive: true });

            this.element.addEventListener("touchend", (e) => {
                this.touchEndX = e.changedTouches[0].screenX;
                this.handleSwipe();
            }, { passive: true });
        }

        handleSwipe() {
            const threshold = 40;
            const diff = this.touchEndX - this.touchStartX;
            if (Math.abs(diff) > threshold) {
                this.pauseAutoplay();
                if (diff < 0) {
                    this.nextSlide(); // Swiped left -> next
                } else {
                    this.prevSlide(); // Swiped right -> prev
                }
            }
        }

        goToSlide(index) {
            if (index === this.currentIndex) return;

            // Remove active state from current
            this.slides[this.currentIndex].classList.remove("cmp-carousel__item--active");
            if (this.indicators[this.currentIndex]) {
                this.indicators[this.currentIndex].classList.remove("cmp-carousel__indicator--active");
                this.indicators[this.currentIndex].setAttribute("tabindex", "-1");
            }

            // Activate target slide
            this.currentIndex = (index + this.slides.length) % this.slides.length;
            this.slides[this.currentIndex].classList.add("cmp-carousel__item--active");
            if (this.indicators[this.currentIndex]) {
                this.indicators[this.currentIndex].classList.add("cmp-carousel__indicator--active");
                this.indicators[this.currentIndex].setAttribute("tabindex", "0");
            }
        }

        nextSlide() {
            this.goToSlide(this.currentIndex + 1);
        }

        prevSlide() {
            this.goToSlide(this.currentIndex - 1);
        }

        startAutoplay() {
            this.clearTimer();
            this.isPlaying = true;
            this.timer = setInterval(() => {
                this.nextSlide();
            }, this.delay);
        }

        pauseAutoplay() {
            this.clearTimer();
            this.isPlaying = false;
        }

        clearTimer() {
            if (this.timer) {
                clearInterval(this.timer);
                this.timer = null;
            }
        }

        updatePauseBtnUI(playing) {
            if (!this.pauseBtn) return;
            const pauseIcon = this.pauseBtn.querySelector(".cmp-carousel__icon--pause");
            const playIcon = this.pauseBtn.querySelector(".cmp-carousel__icon--play");
            if (pauseIcon && playIcon) {
                pauseIcon.style.display = playing ? "inline-block" : "none";
                playIcon.style.display = playing ? "none" : "inline-block";
                this.pauseBtn.setAttribute("aria-label", playing ? "Pause slide rotation" : "Start slide rotation");
                this.pauseBtn.setAttribute("aria-pressed", playing ? "false" : "true");
            }
        }
    }

    function initCarousels() {
        const carousels = document.querySelectorAll('[data-cmp-is="carousel"]');
        carousels.forEach((el) => {
            if (!el.__carouselInitialized) {
                el.__carouselInitialized = new Carousel(el);
            }
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initCarousels);
    } else {
        initCarousels();
    }
})();

