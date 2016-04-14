/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl4;

import oit.framework.BufferUtils;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL4;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;
import glm.glm;
import glm.mat._4.Mat4;
import glm.vec._3.Vec3;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import oit.framework.InputListener;
import oit.framework.Resources;
import oit.gl4.ab.ABuffer;
import oit.gl4.wb.WeightedBlendedOpaque;

/**
 *
 * @author gbarbieri
 */
public class Viewer implements GLEventListener {

    public static void main(String[] args) {

        Display display = NewtFactory.createDisplay(null);
        Screen screen = NewtFactory.createScreen(display, 0);
        GLProfile glProfile = GLProfile.get(GLProfile.GL4);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);
        Resources.glWindow = GLWindow.create(screen, glCapabilities);

        Resources.glWindow.setSize(Resources.imageSize.x, Resources.imageSize.y);
        Resources.glWindow.setPosition(50, 50);
        Resources.glWindow.setUndecorated(false);
        Resources.glWindow.setAlwaysOnTop(false);
        Resources.glWindow.setFullscreen(false);
        Resources.glWindow.setPointerVisible(true);
        Resources.glWindow.confinePointer(false);
        Resources.glWindow.setTitle("Weighted Blended");
        Resources.glWindow.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
        Resources.glWindow.setVisible(true);

        Viewer viewer = new Viewer();
        Resources.glWindow.addGLEventListener(viewer);

        InputListener inputListener = new InputListener();
        Resources.glWindow.addMouseListener(inputListener);
        Resources.glWindow.addKeyListener(inputListener);

        Resources.animator = new Animator(Resources.glWindow);
        Resources.animator.setRunAsFastAsPossible(true);
        Resources.animator.setExclusiveContext(true);
        Resources.animator.start();
    }

    public class Buffer {

        public static final int TRANSFORM0 = 0;
        public static final int TRANSFORM1 = 1;
        public static final int TRANSFORM2 = 2;
        public static final int PARAMETERS = 3;
        public static final int MAX = 4;
    }

    public static IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);
    private Scene scene;
    private WeightedBlendedOpaque weightedBlendedOpaque;
    private ABuffer aBuffer;
    private Mat4 proj = new Mat4(), view = new Mat4();
    /**
     * https://jogamp.org/bugzilla/show_bug.cgi?id=1287
     */
    private boolean bug1287 = true;

    @Override
    public void init(GLAutoDrawable glad) {

        GL4 gl4 = glad.getGL().getGL4();

        initDebug(gl4);

        initBuffers(gl4);

        scene = new Scene(gl4);

//        weightedBlendedOpaque = new WeightedBlendedOpaque(gl4);
        aBuffer = new ABuffer(gl4);

        gl4.glDisable(GL4.GL_CULL_FACE);
    }

    private void initDebug(GL4 gl4) {

        Resources.glWindow.getContext().addGLDebugListener(new GlDebugOutput());
        // Turn off all the debug
        gl4.glDebugMessageControl(
                GL_DONT_CARE, // source
                GL_DONT_CARE, // type
                GL_DONT_CARE, // severity
                0, // count
                null, // id
                false); // enabled
        // Turn on all OpenGL Errors, shader compilation/linking errors, or highly-dangerous undefined behavior 
        gl4.glDebugMessageControl(
                GL_DONT_CARE, // source
                GL_DONT_CARE, // type
                GL_DEBUG_SEVERITY_HIGH, // severity
                0, // count
                null, // id
                true); // enabled
        // Turn on all major performance warnings, shader compilation/linking warnings or the use of deprecated functions
        gl4.glDebugMessageControl(
                GL_DONT_CARE, // source
                GL_DONT_CARE, // type
                GL_DEBUG_SEVERITY_MEDIUM, // severity
                0, // count
                null, // id
                true); // enabled
    }

    private void initBuffers(GL4 gl4) {

        gl4.glCreateBuffers(Buffer.MAX, bufferName);

        if (!bug1287) {

            gl4.glNamedBufferStorage(bufferName.get(Buffer.TRANSFORM0), Mat4.SIZE * 2, null, GL_DYNAMIC_STORAGE_BIT);

            gl4.glNamedBufferStorage(bufferName.get(Buffer.TRANSFORM1), Mat4.SIZE, null, GL_DYNAMIC_STORAGE_BIT);

            gl4.glNamedBufferStorage(bufferName.get(Buffer.TRANSFORM2), Mat4.SIZE, null, GL_DYNAMIC_STORAGE_BIT);

            gl4.glNamedBufferStorage(bufferName.get(Buffer.PARAMETERS), 2 * Float.BYTES, null, GL_DYNAMIC_STORAGE_BIT);

        } else {

            gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM0));
            gl4.glBufferStorage(GL_UNIFORM_BUFFER, Mat4.SIZE * 2, null, GL_DYNAMIC_STORAGE_BIT);

            gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM1));
            gl4.glBufferStorage(GL_UNIFORM_BUFFER, Mat4.SIZE, null, GL_DYNAMIC_STORAGE_BIT);

            Mat4 modelToClip = glm.ortho_(0, 1, 0, 1);
            ByteBuffer buffer = GLBuffers.newDirectByteBuffer(Mat4.SIZE);
            buffer.asFloatBuffer().put(modelToClip.toFa_());
            gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM2));
            gl4.glBufferStorage(GL_UNIFORM_BUFFER, Mat4.SIZE, buffer, GL_DYNAMIC_STORAGE_BIT);

            gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.PARAMETERS));
            gl4.glBufferStorage(GL_UNIFORM_BUFFER, 2 * Float.BYTES, null, GL_DYNAMIC_STORAGE_BIT);

            BufferUtils.destroyDirectBuffer(buffer);
        }
        gl4.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM0, bufferName.get(Buffer.TRANSFORM0));
        gl4.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM1, bufferName.get(Buffer.TRANSFORM1));
        gl4.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM2, bufferName.get(Buffer.TRANSFORM2));
        gl4.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.PARAMETERS, bufferName.get(Buffer.PARAMETERS));
    }

    @Override
    public void dispose(GLAutoDrawable glad) {

        GL4 gl4 = glad.getGL().getGL4();
//        weightedBlendedOpaque.dispose(gl4);

        System.exit(0);
    }

    @Override
    public void display(GLAutoDrawable glad) {

        GL4 gl4 = glad.getGL().getGL4();

        {
            glm.lookAt(InputListener.pos, new Vec3(InputListener.pos.x, InputListener.pos.y, 0), new Vec3(0, 1, 0), view)
                    .rotate((float) Math.toRadians(InputListener.rot.x), 1, 0, 0)
                    .rotate((float) Math.toRadians(InputListener.rot.y), 0, 1, 0)
                    .translate(ModelGL4.trans[0], ModelGL4.trans[1], ModelGL4.trans[2])
                    .scale(ModelGL4.scale);

            view.toFb(Resources.matBuffer);

            gl4.glNamedBufferSubData(bufferName.get(Buffer.TRANSFORM0), 0, Mat4.SIZE, Resources.matBuffer);
        }
        {
            Resources.parameters.putFloat(0 * Float.BYTES, Resources.opacity);
            Resources.parameters.putFloat(1 * Float.BYTES, Resources.weight);

            gl4.glNamedBufferSubData(bufferName.get(Buffer.PARAMETERS), 0, Float.BYTES * 2, Resources.parameters);
        }

//        weightedBlendedOpaque.render(gl4, scene);
        aBuffer.render(gl4, scene);
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int width, int height) {

        GL4 gl4 = glad.getGL().getGL4();

//        weightedBlendedOpaque.reshape(gl4);
        aBuffer.reshape(gl4);

        Resources.imageSize.set(width, height);

        glm.perspective((float) Math.toRadians(30f), (float) width / height, 0.0001f, 10, proj);
        proj.toFb(Resources.matBuffer);
        gl4.glNamedBufferSubData(bufferName.get(Buffer.TRANSFORM0), Mat4.SIZE, Mat4.SIZE, Resources.matBuffer);

        gl4.glViewport(0, 0, width, height);
    }
}