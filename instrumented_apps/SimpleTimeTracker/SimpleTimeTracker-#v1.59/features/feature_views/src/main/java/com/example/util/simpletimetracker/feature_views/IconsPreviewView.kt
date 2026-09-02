package com.example.util.simpletimetracker.feature_views

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.View
import com.example.util.simpletimetracker.feature_views.extension.getBitmapFromView
import com.example.util.simpletimetracker.feature_views.extension.measureExactly
import com.example.util.simpletimetracker.feature_views.viewData.RecordTypeIcon
import kotlin.math.ceil
import androidx.core.content.withStyledAttributes
import com.example.util.simpletimetracker.feature_views.ParticlesAnimationUtils.interpolate
import com.example.util.simpletimetracker.feature_views.ParticlesAnimationUtils.pseudoRandom

class IconsPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(
    context,
    attrs,
    defStyleAttr,
) {

    // Attrs
    private var iconMaxSize: Int = 0
    private var iconColor: Int = 0
    // End of attrs

    private val particlePaint: Paint = Paint()
    private val gradientPaint: Paint = Paint()
    private val particlesAppearAnimator = ValueAnimator.ofFloat(0f, 1f)
    private val particleBounds: RectF = RectF(0f, 0f, 0f, 0f)
    private var iconId: RecordTypeIcon? = null
    private var icon: Bitmap? = null
    private var particlesAppearAnimationAlpha: Float = 1f
    private var shouldAnimateParticlesAppearing: Boolean = true
    private val iconView: IconView = IconView(ContextThemeWrapper(context, R.style.AppTheme))

    init {
        initArgs(context, attrs, defStyleAttr)
        initPaint()
        initEditMode()
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = resolveSize(0, widthMeasureSpec)
        val h = resolveSize(w, heightMeasureSpec)

        setMeasuredDimension(w, h)
        initGradientPaint(w.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        if (icon == null) return

        val w = width.toFloat()
        val h = height.toFloat()

        drawParticles(
            drawable = icon,
            canvas = canvas,
            w = w,
            h = h,
        )
    }

    fun setIcon(
        iconId: RecordTypeIcon,
    ) {
        if (this.iconId != iconId) {
            animateParticlesStartTime = -1
            shouldAnimateParticlesAppearing = true
            this.iconId = iconId
        }
        icon = getIconDrawable(iconId)
        invalidate()
        if (!isInEditMode) {
            animateParticlesAppearing()
        }
    }

    private fun initArgs(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) {
        context
            .withStyledAttributes(
                attrs,
                R.styleable.IconsPreviewView, defStyleAttr, 0,
            ) {
                iconMaxSize =
                    getDimensionPixelSize(R.styleable.IconsPreviewView_iconsPreviewMaxSize, 0)
                iconColor =
                    getColor(R.styleable.IconsPreviewView_iconsPreviewColor, Color.BLACK)
            }
    }

    private fun initPaint() {
        gradientPaint.apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
    }

    @Suppress("UnnecessaryVariable")
    @SuppressLint("UseKtx")
    private fun drawParticles(
        drawable: Bitmap?,
        canvas: Canvas,
        w: Float,
        h: Float,
    ) {
        if (disableAnimationsForTest) return
        if (drawable == null) return

        if (animateParticlesStartTime == -1L) {
            animateParticlesStartTime = System.currentTimeMillis()
        }
        val time = animateParticlesStartTime / PARTICLES_CYCLE

        val iconSizeHalfSize = iconMaxSize / 2f
        particleBounds.set(
            -iconSizeHalfSize, -iconSizeHalfSize,
            iconSizeHalfSize, iconSizeHalfSize,
        )

        val fromPosition = 0
        val toPosition = ceil(h / PARTICLES_POSITION_STEP).toInt()

        for (i in fromPosition..toPosition) {
            val position = i * PARTICLES_POSITION_STEP

            val startTimeVariation = PARTICLES_CYCLE * 100 * pseudoRandom(position)
            val speedVariation = PARTICLES_BASE_SPEED + PARTICLES_SPEED_VARIATION * pseudoRandom(position)
            val distanceVariation = ((time + startTimeVariation) * speedVariation) % 1.0

            val innerParticleSpanDistance = -iconSizeHalfSize
            val outerParticleSpanDistance = w + iconSizeHalfSize
            val particleDistance = interpolate(
                0f, outerParticleSpanDistance, distanceVariation,
            )

            if (
                particleDistance > innerParticleSpanDistance &&
                particleDistance < outerParticleSpanDistance
            ) {
                val alpha = PARTICLES_BASE_ALPHA + PARTICLES_ALPHA_VARIATION * pseudoRandom(position + 1)
                val scale = PARTICLES_BASE_SCALE + PARTICLES_SCALE_VARIATION * pseudoRandom(position + 2)
                val rotation = PARTICLES_BASE_ROTATION + PARTICLES_ROTATION_VARIATION * pseudoRandom(position + 3)
                val x = w - particleDistance
                val y = position
                particlePaint.alpha = (0xFF * alpha * particlesAppearAnimationAlpha).toInt()

                canvas.save()
                canvas.translate(x.toFloat(), y.toFloat())
                canvas.scale(scale.toFloat(), scale.toFloat())
                canvas.rotate(rotation.toFloat())
                canvas.drawBitmap(drawable, null, particleBounds, particlePaint)
                canvas.restore()
            }
        }
        canvas.drawRect(RectF(0f, 0f, w, h), gradientPaint)
    }

    private fun initGradientPaint(w: Float) {
        val gradientShader = LinearGradient(
            0f, 0f, w, 0f,
            Color.TRANSPARENT, Color.WHITE,
            Shader.TileMode.CLAMP,
        )
        gradientPaint.shader = gradientShader
    }

    private fun initEditMode() {
        if (isInEditMode) {
            setIcon(iconId = RecordTypeIcon.Image(R.drawable.unknown))
        }
    }

    private fun getIconDrawable(iconId: RecordTypeIcon): Bitmap {
        return iconView
            .apply {
                itemIcon = iconId
                itemIconColor = this@IconsPreviewView.iconColor
                measureExactly(this@IconsPreviewView.iconMaxSize)
            }
            .getBitmapFromView()
    }

    private fun animateParticlesAppearing() {
        if (shouldAnimateParticlesAppearing) {
            particlesAppearAnimator.duration = PARTICLES_APPEAR_ANIMATION_DURATION_MS
            particlesAppearAnimator.addUpdateListener { animation ->
                particlesAppearAnimationAlpha = animation.animatedValue as Float
                invalidate()
            }
            particlesAppearAnimator.start()
            shouldAnimateParticlesAppearing = false
        }
    }

    companion object {
        var disableAnimationsForTest: Boolean = false

        private var animateParticlesStartTime: Long = -1

        private const val PARTICLES_APPEAR_ANIMATION_DURATION_MS: Long = 500L

        private const val PARTICLES_POSITION_STEP: Double = 5.0
        private const val PARTICLES_CYCLE: Double = 30000.0
        private const val PARTICLES_BASE_SPEED: Double = 1.0
        private const val PARTICLES_SPEED_VARIATION: Double = 1.0
        private const val PARTICLES_BASE_ALPHA: Double = 1.0
        private const val PARTICLES_ALPHA_VARIATION: Double = -0.25
        private const val PARTICLES_BASE_SCALE: Double = 1.0
        private const val PARTICLES_SCALE_VARIATION: Double = -0.25
        private const val PARTICLES_BASE_ROTATION: Double = -10.0
        private const val PARTICLES_ROTATION_VARIATION: Double = 20.0
    }
}