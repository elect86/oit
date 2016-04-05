/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3.dp.glsl;

import com.jogamp.opengl.GL3;
import oit.gl3.Semantic;

/**
 *
 * @author gbarbieri
 */
public class Peel extends glsl.GLSLProgramObject {

    private int depthTexUL;
    private int modelToWorldUL;
    private int alphaUL;

    public Peel(GL3 gl3, String shadersFilepath, String[] vertexShaders, String[] fragmentShaders) {

        super(gl3, shadersFilepath, vertexShaders, fragmentShaders);

        int projectionUBI = gl3.glGetUniformBlockIndex(getProgramId(), "vpMatrixes");
        gl3.glUniformBlockBinding(getProgramId(), projectionUBI, Semantic.Uniform.TRANSFORM0);

        depthTexUL = gl3.glGetUniformLocation(getProgramId(), "depthTex");

        modelToWorldUL = gl3.glGetUniformLocation(getProgramId(), "modelToWorld");

        alphaUL = gl3.glGetUniformLocation(getProgramId(), "alpha");

        if (projectionUBI == -1 || depthTexUL == -1 || alphaUL == -1 || modelToWorldUL == -1) {
            System.out.println("[Peel] UL error");
        }
    }

    public int getDepthTexUL() {
        return depthTexUL;
    }

    public int getModelToWorldUL() {
        return modelToWorldUL;
    }

    public int getAlphaUL() {
        return alphaUL;
    }
}
