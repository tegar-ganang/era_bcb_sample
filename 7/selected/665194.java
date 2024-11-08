package app;

import java.io.*;
import java.util.*;
import java.lang.Math;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import app.TextureReader;
import app.Robot;

public class Robot implements RobotInterface {

    GL gl;

    float cameraDistance = 30;

    public float energy = 100f;

    public float health = 100f;

    public float armor;

    float max_speed = 1f;

    float speed = 0.2f;

    float maxIncline = 5f;

    int ammo = 0;

    float verticalVelocity = 0;

    float robotDirection = 0;

    private GLU glu = new GLU();

    public Vector3 forwardDirection = null;

    public Vector3 cameraDirection = null;

    public Vector3 position = null;

    public Vector3 goal = null;

    public Vector3 gunDirection = null;

    RobotList<Enemy> enemy_list = new RobotList<Enemy>(Enemy.class);

    RobotList<Resource> resource_list = new RobotList<Resource>(Resource.class);

    Vector3 direction = null;

    Terrain terrain;

    Sphere sphere = null;

    private int robotFace;

    private int robotTexture;

    float oldTime = 0;

    float maxEnergy = 100;

    Vector3 shootDirection = null;

    public boolean shoot = false;

    float healthArmorStat = 1.0f;

    float weaponStat = 1.0f;

    float speedStat = 1.0f;

    public FuncSet funcset = new FuncSet();

    public Robot(Terrain t) {
        forwardDirection = new Vector3(0, 0, -1);
        cameraDirection = new Vector3(0, 10, -10);
        position = new Vector3(0f, 0f, 0f);
        goal = new Vector3(0, 0, 0);
        terrain = t;
        health = (int) (100f * healthArmorStat);
        max_speed = 1.0f * speedStat;
        maxIncline = 1.0f * speedStat;
    }

    public Robot(GL g, Terrain t) {
        gl = g;
        forwardDirection = new Vector3(0, 0, -10);
        terrain = t;
        forwardDirection = new Vector3(0, 0, -1);
        cameraDirection = new Vector3(0, 10, -1);
        position = new Vector3(0f, 0f, 0f);
        goal = new Vector3(0, 0, 0);
        gunDirection = new Vector3(1, -1, 0);
        sphere = new Sphere(gl);
        robotFace = genTexture(gl);
        gl.glBindTexture(GL.GL_TEXTURE_2D, robotFace);
        TextureReader.Texture texture = null;
        gl.glEnable(GL.GL_TEXTURE_2D);
        try {
            texture = TextureReader.readTexture("media/robot/robotFace.png");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        makeRGBTexture(gl, glu, texture, GL.GL_TEXTURE_2D, false);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        robotTexture = genTexture(gl);
        gl.glBindTexture(GL.GL_TEXTURE_2D, robotTexture);
        texture = null;
        gl.glEnable(GL.GL_TEXTURE_2D);
        try {
            texture = TextureReader.readTexture("media/robot/robotBody.png");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        makeRGBTexture(gl, glu, texture, GL.GL_TEXTURE_2D, false);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
    }

    public Robot() {
        position = new Vector3(0f, 0f, 0f);
    }

    public void init(Terrain t) {
        forwardDirection = new Vector3(0, 0, -1);
        cameraDirection = new Vector3(0, 10, -10);
        position = new Vector3(0f, 0f, 0f);
        goal = new Vector3(0, 0, 0);
        terrain = t;
    }

    private void makeRGBTexture(GL gl, GLU glu, TextureReader.Texture img, int target, boolean mipmapped) {
        if (mipmapped) {
            glu.gluBuild2DMipmaps(target, GL.GL_RGB8, img.getWidth(), img.getHeight(), GL.GL_RGB, GL.GL_UNSIGNED_BYTE, img.getPixels());
        } else {
            gl.glTexImage2D(target, 0, GL.GL_RGB, img.getWidth(), img.getHeight(), 0, GL.GL_RGB, GL.GL_UNSIGNED_BYTE, img.getPixels());
        }
    }

    private int genTexture(GL gl) {
        final int[] tmp = new int[1];
        gl.glGenTextures(1, tmp, 0);
        return tmp[0];
    }

    boolean firstRun = true;

    public void update(float time, Vector3 lookat) {
        if (firstRun == true) {
            firstRun = false;
            position.y = terrain.terrainIntersection(new Vector3(position.x, position.z, 0));
        }
        forwardDirection = new Vector3(goal.x - position.x, 0, goal.z - position.z);
        forwardDirection.normalize();
        float timeInterval = time - oldTime;
        oldTime = time;
        float distance = timeInterval * speed;
        float realDistance = (float) Math.sqrt(Math.pow(position.x - goal.x, 2) + Math.pow(position.z - goal.z, 2));
        if (realDistance < distance) distance = realDistance;
        float directionRadians = this.direction(position, goal);
        Vector3 newPosition = new Vector3((float) (position.x + distance * forwardDirection.x), (float) (position.z + distance * forwardDirection.z), (float) 0);
        float newY = terrain.terrainIntersection(new Vector3(newPosition.x, newPosition.y, 0));
        float nextY = terrain.terrainIntersection(new Vector3(newPosition.x + forwardDirection.x * 0.01f, newPosition.y + forwardDirection.z * 0.01f, 0));
        float incline = (nextY - newY) / 0.01f;
        if (goal.x != position.x || goal.z != position.z) {
            {
                position.x = newPosition.x;
                position.z = newPosition.y;
                position.y = newY;
            }
        }
        float tempx = position.x + cameraDistance * (float) Math.cos((double) time / 100.0);
        float tempy = position.z + cameraDistance * (float) Math.sin((double) time / 100.0);
        if (lookat != null) {
            Vector3 lookFrom = new Vector3(lookat.x - position.x, lookat.y - position.y, lookat.z - position.z);
            lookFrom.normalize();
            tempx = position.x + cameraDistance * (lookFrom.x);
            tempy = position.z + cameraDistance * (lookFrom.z);
        }
        cameraDirection = new Vector3(tempx, position.y + cameraDistance / 2.0f, tempy);
    }

    public void renderRobot(GL gl, float time, boolean walking, float direction) {
        gl.glRotatef(direction, 0.0f, 1.0f, 0.0f);
        if (walking) {
            gl.glPushMatrix();
            renderRobotWalking(gl, time);
            gl.glPopMatrix();
        } else {
            gl.glPushMatrix();
            renderRobotIdle(gl, time);
            gl.glPopMatrix();
        }
    }

    private void renderRobotIdle(GL gl, float time) {
        gl.glBindTexture(GL.GL_TEXTURE_2D, robotFace);
        float scale = 0.5f;
        gl.glScalef(scale, scale, scale);
        gl.glPushMatrix();
        float height = (float) (Math.cos(time) * Math.cos(time + 3.14f));
        gl.glTranslatef(0, 7 + height * 0.45f, 0);
        sphere.renderSphere(0, 0, 0, 0.5f, 15);
        gl.glBindTexture(GL.GL_TEXTURE_2D, robotTexture);
        gl.glTranslatef(0, -1.3f - height * 0.05f, 0);
        sphere.renderSphere(0, 0, 0, 0.8f, 15);
        gl.glPushMatrix();
        float leftArm = (float) Math.cos(time);
        gl.glTranslatef(1.1f, 0.4f, 0);
        sphere.renderSphere(0, 0, 0, 0.4f, 15);
        gl.glTranslatef(0.2f, -0.6f - height * 0.05f, 0);
        sphere.renderSphere(0, 0, 0, 0.25f, 15);
        gl.glTranslatef(0.1f, -0.5f - height * 0.05f, 0);
        sphere.renderSphere(0, 0, 0, 0.225f, 15);
        gl.glTranslatef(0.1f, -0.4f - height * 0.05f, 0);
        sphere.renderSphere(0, 0, 0, 0.22f, 15);
        gl.glTranslatef(0.1f, -0.4f - height * 0.05f, 0);
        sphere.renderSphere(0, 0, 0, 0.215f, 15);
        gl.glPopMatrix();
        gl.glPushMatrix();
        float rightArm = (float) Math.cos(time + 3.14f);
        gl.glTranslatef(-1.1f, 0.4f, 0);
        sphere.renderSphere(0, 0, 0, 0.4f, 15);
        gl.glTranslatef(-0.2f, -0.6f - height * 0.05f, 0);
        sphere.renderSphere(0, 0, 0, 0.25f, 15);
        gl.glTranslatef(-0.1f, -0.5f - height * 0.05f, 0);
        sphere.renderSphere(0, 0, 0, 0.225f, 15);
        gl.glTranslatef(-0.1f, -0.4f - height * 0.05f, 0);
        sphere.renderSphere(0, 0, 0, 0.22f, 15);
        gl.glTranslatef(-0.1f, -0.4f - height * 0.05f, 0);
        sphere.renderSphere(0, 0, 0, 0.215f, 15);
        gl.glPopMatrix();
        gl.glTranslatef(0, -1.0f, 0);
        sphere.renderSphere(0, 0, 0, 0.5f, 15);
        gl.glTranslatef(0, -0.9f - height * 0.05f, 0);
        sphere.renderSphere(0, 0, 0, 0.3f, 15);
        gl.glPushMatrix();
        float leftLeg = (float) Math.cos(time + 1.57f);
        gl.glTranslatef(0.6f, -0.2f - height * 0.05f, 0);
        sphere.renderSphere(0, 0, 0, 0.3f, 15);
        gl.glTranslatef(0.1f, -0.6f - height * 0.05f, 0);
        sphere.renderSphere(0, 0, 0, 0.3f, 15);
        gl.glTranslatef(0.05f, -0.6f - height * 0.05f, 0);
        sphere.renderSphere(0, 0, 0, 0.25f, 15);
        gl.glTranslatef(0.025f, -0.55f - height * 0.05f, 0);
        sphere.renderSphere(0, 0, 0, 0.225f, 15);
        gl.glTranslatef(0.0125f, -0.45f - height * 0.05f, 0);
        sphere.renderSphere(0, 0, 0, 0.27f, 15);
        gl.glTranslatef(0.0f, -0.45f - height * 0.05f, 0);
        sphere.renderSphere(0, 0, 0, 0.31f, 15);
        gl.glPopMatrix();
        gl.glPushMatrix();
        float rightLeg = (float) Math.cos(time + 1.57f + 3.14f);
        gl.glTranslatef(-0.6f, -0.2f - height * 0.05f, 0);
        sphere.renderSphere(0, 0, 0, 0.3f, 15);
        gl.glTranslatef(-0.1f, -0.6f - height * 0.05f, 0);
        sphere.renderSphere(0, 0, 0, 0.3f, 15);
        gl.glTranslatef(-0.05f, -0.6f - height * 0.05f, 0);
        sphere.renderSphere(0, 0, 0, 0.25f, 15);
        gl.glTranslatef(-0.025f, -0.55f - height * 0.05f, 0);
        sphere.renderSphere(0, 0, 0, 0.225f, 15);
        gl.glTranslatef(-0.0125f, -0.45f - height * 0.05f, 0);
        sphere.renderSphere(0, 0, 0, 0.27f, 15);
        gl.glTranslatef(-0.0f, -0.45f - height * 0.05f, 0);
        sphere.renderSphere(0, 0, 0, 0.31f, 15);
        gl.glPopMatrix();
        gl.glPopMatrix();
    }

    private void renderRobotWalking(GL gl, float time) {
        gl.glBindTexture(GL.GL_TEXTURE_2D, robotFace);
        float scale = 0.5f;
        gl.glScalef(scale, scale, scale);
        gl.glPushMatrix();
        float height = (float) (Math.cos(time) * Math.cos(time + 3.14f));
        gl.glTranslatef(0, 7 + height, 0);
        sphere.renderSphere(0, 0, 0, 0.5f, 15);
        gl.glBindTexture(GL.GL_TEXTURE_2D, robotTexture);
        gl.glTranslatef(0, -1.3f, 0);
        sphere.renderSphere(0, 0, 0, 0.8f, 15);
        gl.glPushMatrix();
        float leftArm = (float) Math.cos(time);
        gl.glTranslatef(1.1f, 0.4f, leftArm * 0.5f);
        sphere.renderSphere(0, 0, 0, 0.4f, 15);
        gl.glTranslatef(0.2f, -0.6f, leftArm * 0.4f);
        sphere.renderSphere(0, 0, 0, 0.25f, 15);
        gl.glTranslatef(0.1f, -0.5f, leftArm * 0.45f);
        sphere.renderSphere(0, 0, 0, 0.225f, 15);
        gl.glTranslatef(0.1f, -0.4f, leftArm * 0.5f);
        sphere.renderSphere(0, 0, 0, 0.22f, 15);
        gl.glTranslatef(0.1f, -0.4f, leftArm * 0.6f);
        sphere.renderSphere(0, 0, 0, 0.215f, 15);
        gl.glPopMatrix();
        gl.glPushMatrix();
        float rightArm = (float) Math.cos(time + 3.14f);
        gl.glTranslatef(-1.1f, 0.4f, rightArm * 0.5f);
        sphere.renderSphere(0, 0, 0, 0.4f, 15);
        gl.glTranslatef(-0.2f, -0.6f, rightArm * 0.4f);
        sphere.renderSphere(0, 0, 0, 0.25f, 15);
        gl.glTranslatef(-0.1f, -0.5f, rightArm * 0.45f);
        sphere.renderSphere(0, 0, 0, 0.225f, 15);
        gl.glTranslatef(-0.1f, -0.4f, rightArm * 0.5f);
        sphere.renderSphere(0, 0, 0, 0.22f, 15);
        gl.glTranslatef(-0.1f, -0.4f, rightArm * 0.6f);
        sphere.renderSphere(0, 0, 0, 0.215f, 15);
        gl.glPopMatrix();
        gl.glTranslatef(0, -1.0f, 0);
        sphere.renderSphere(0, 0, 0, 0.5f, 15);
        gl.glTranslatef(0, -0.9f, 0);
        sphere.renderSphere(0, 0, 0, 0.3f, 15);
        gl.glPushMatrix();
        float leftLeg = (float) Math.cos(time + 1.57f);
        gl.glTranslatef(0.6f, -0.2f - leftLeg * 0.1f, rightArm * 0.1f);
        sphere.renderSphere(0, 0, 0, 0.3f, 15);
        gl.glTranslatef(0.1f, -0.6f - leftLeg * 0.1f, rightArm * 0.25f);
        sphere.renderSphere(0, 0, 0, 0.3f, 15);
        gl.glTranslatef(0.05f, -0.6f - leftLeg * 0.1f, rightArm * 0.35f);
        sphere.renderSphere(0, 0, 0, 0.25f, 15);
        gl.glTranslatef(0.025f, -0.55f - leftLeg * 0.1f, rightArm * 0.475f);
        sphere.renderSphere(0, 0, 0, 0.225f, 15);
        gl.glTranslatef(0.0125f, -0.45f - leftLeg * 0.1f, rightArm * 0.6f);
        sphere.renderSphere(0, 0, 0, 0.27f, 15);
        gl.glTranslatef(0.0f, -0.45f - leftLeg * 0.1f, rightArm * 0.7f);
        sphere.renderSphere(0, 0, 0, 0.31f, 15);
        gl.glPopMatrix();
        gl.glPushMatrix();
        float rightLeg = (float) Math.cos(time + 1.57f + 3.14f);
        gl.glTranslatef(-0.6f, -0.2f - rightLeg * 0.1f, leftArm * 0.1f);
        sphere.renderSphere(0, 0, 0, 0.3f, 15);
        gl.glTranslatef(-0.1f, -0.6f - rightLeg * 0.1f, leftArm * 0.25f);
        sphere.renderSphere(0, 0, 0, 0.3f, 15);
        gl.glTranslatef(-0.05f, -0.6f - rightLeg * 0.1f, leftArm * 0.35f);
        sphere.renderSphere(0, 0, 0, 0.25f, 15);
        gl.glTranslatef(-0.025f, -0.55f - rightLeg * 0.1f, leftArm * 0.475f);
        sphere.renderSphere(0, 0, 0, 0.225f, 15);
        gl.glTranslatef(-0.0125f, -0.45f - rightLeg * 0.1f, leftArm * 0.6f);
        sphere.renderSphere(0, 0, 0, 0.27f, 15);
        gl.glTranslatef(-0.0f, -0.45f - rightLeg * 0.1f, leftArm * 0.7f);
        sphere.renderSphere(0, 0, 0, 0.31f, 15);
        gl.glPopMatrix();
        gl.glPopMatrix();
    }

    public void setY(int Y) {
        position.y = Y;
    }

    public Vector3 getPosition() {
        return new Vector3(position.x, position.y, position.z);
    }

    public void think() {
        ping();
        RobotList<Enemy> enemies = sort_incr_Enemy(get_enemies(), "");
        RobotList<Resource> res = sort_incr_Resource(get_resources(), "");
        if (energy > 5) {
            move_to(funcset.minus(enemies.get(1f).location));
            for (Resource e : res) {
                say(e.toString());
            }
        } else {
            move_to(new Location(15f, 15f));
        }
    }

    public void say(String s) {
        String l = Global.outputArea.getText();
        Global.outputArea.setText(s.concat("\n").concat(l));
    }

    public void move_to(Location location) {
        goal = location.getVector3();
        speed = 1;
    }

    public void move_to(Location location, Percentage my_speed) {
        goal = location.getVector3();
        speed = my_speed.percent / 100f;
    }

    public void move_to(Location location, float my_speed) {
        move_to(location, new Percentage(my_speed));
    }

    public void shoot(Enemy en) {
        if (energy > 9) {
            energy = energy - 3;
            Vector3 las_loc = this.getLocation().getVector3();
            shoot = true;
            shootDirection = en.location.getVector3().add(las_loc.multiply(-1));
        } else {
            say("I am running out of energy");
        }
    }

    public void shoot(Location loc) {
        if (energy > 9) {
            energy = energy - 3;
            Vector3 las_loc = this.getLocation().getVector3();
            shoot = true;
            shootDirection = loc.getVector3().add(las_loc.multiply(-1));
        }
    }

    public void ping() {
        if (energy > 4) {
            enemy_list.clear();
            for (Enemy e : Global.enemy_list) {
                enemy_list.addLast(e.copy());
            }
            resource_list.clear();
            for (Resource r : Global.resource_list) {
                resource_list.addLast(r.copy());
            }
        } else {
            say("I am running out of energy");
        }
    }

    public float distance(Location origin, Location goal) {
        return (float) Math.sqrt(Math.pow(origin.x - goal.x, 2) + Math.pow(origin.y - goal.y, 2));
    }

    public float direction(Vector3 origin, Vector3 goal) {
        forwardDirection = new Vector3(goal.x - origin.x, 0, goal.z - origin.z);
        forwardDirection.normalize();
        return (float) Math.atan2((double) (origin.z - goal.z), (double) (origin.x - goal.x));
    }

    public Percentage getEnergy() {
        return new Percentage(this.energy);
    }

    public RobotList<Enemy> get_enemies() {
        return enemy_list;
    }

    public RobotList<Resource> get_resources() {
        return resource_list;
    }

    public void get_environment_height() {
    }

    public RobotList sort_incr(RobotList list, String field) {
        if (list.clazz.equals(Float.class)) {
            return sort_incr_Float(list, field);
        } else if (list.clazz.equals(Resource.class)) {
            return sort_incr_Resource(list, field);
        } else if (list.clazz.equals(Enemy.class)) {
            return sort_incr_Enemy(list, field);
        } else if (list.clazz.equals(Percentage.class)) {
            return sort_incr_Percentage(list, field);
        } else if (list.clazz.equals(Location.class)) {
            return sort_incr_Location(list, field);
        } else {
            say("impossible to sort list - nothing modified");
        }
        return list;
    }

    public RobotList sort_decr(RobotList list, String field) {
        if (list.clazz.equals(Float.class)) {
            return sort_decr_Float(list, field);
        } else if (list.clazz.equals(Resource.class)) {
            return sort_decr_Resource(list, field);
        } else if (list.clazz.equals(Enemy.class)) {
            return sort_decr_Enemy(list, field);
        } else if (list.clazz.equals(Percentage.class)) {
            return sort_decr_Percentage(list, field);
        } else if (list.clazz.equals(Location.class)) {
            return sort_decr_Location(list, field);
        } else {
            say("impossible to sort list - nothing modified");
        }
        return list;
    }

    public RobotList<Float> sort_incr_Float(RobotList<Float> list, String field) {
        int length = list.size();
        Index_value[] distri = new Index_value[length];
        for (int i = 0; i < length; i++) {
            distri[i] = new Index_value(i, list.get(i));
        }
        boolean permut;
        do {
            permut = false;
            for (int i = 0; i < length - 1; i++) {
                if (distri[i].value > distri[i + 1].value) {
                    Index_value a = distri[i];
                    distri[i] = distri[i + 1];
                    distri[i + 1] = a;
                    permut = true;
                }
            }
        } while (permut);
        RobotList<Float> sol = new RobotList<Float>(Float.class);
        for (int i = 0; i < length; i++) {
            sol.addLast(new Float(distri[i].value));
        }
        return sol;
    }

    public RobotList<Float> sort_decr_Float(RobotList<Float> list, String field) {
        int length = list.size();
        Index_value[] distri = new Index_value[length];
        for (int i = 0; i < length; i++) {
            distri[i] = new Index_value(i, list.get(i));
        }
        boolean permut;
        do {
            permut = false;
            for (int i = 0; i < length - 1; i++) {
                if (distri[i].value < distri[i + 1].value) {
                    Index_value a = distri[i];
                    distri[i] = distri[i + 1];
                    distri[i + 1] = a;
                    permut = true;
                }
            }
        } while (permut);
        RobotList<Float> sol = new RobotList<Float>(Float.class);
        for (int i = 0; i < length; i++) {
            sol.addLast(new Float(distri[i].value));
        }
        return sol;
    }

    public RobotList<Enemy> sort_incr_Enemy(RobotList<Enemy> list, String field) {
        int length = list.size();
        Index_value[] enemy_dist = new Index_value[length];
        if (field.equals("") || field.equals("location")) {
            Location cur_loc = this.getLocation();
            for (int i = 0; i < length; i++) {
                enemy_dist[i] = new Index_value(i, distance(cur_loc, list.get(i).location));
            }
        } else if (field.equals("health")) {
            for (int i = 0; i < length; i++) {
                enemy_dist[i] = new Index_value(i, list.get(i).health);
            }
        } else {
            say("impossible to sort list - nothing modified");
            return list;
        }
        boolean permut;
        do {
            permut = false;
            for (int i = 0; i < length - 1; i++) {
                if (enemy_dist[i].value > enemy_dist[i + 1].value) {
                    Index_value a = enemy_dist[i];
                    enemy_dist[i] = enemy_dist[i + 1];
                    enemy_dist[i + 1] = a;
                    permut = true;
                }
            }
        } while (permut);
        RobotList<Enemy> new_enemy_list = new RobotList<Enemy>(Enemy.class);
        for (int i = 0; i < length; i++) {
            new_enemy_list.addLast(list.get(enemy_dist[i].index));
        }
        return new_enemy_list;
    }

    public RobotList<Enemy> sort_decr_Enemy(RobotList<Enemy> list, String field) {
        int length = list.size();
        Index_value[] enemy_dist = new Index_value[length];
        if (field.equals("") || field.equals("location")) {
            Location cur_loc = this.getLocation();
            for (int i = 0; i < length; i++) {
                enemy_dist[i] = new Index_value(i, distance(cur_loc, list.get(i).location));
            }
        } else if (field.equals("health")) {
            for (int i = 0; i < length; i++) {
                enemy_dist[i] = new Index_value(i, list.get(i).health);
            }
        } else {
            say("impossible to sort list - nothing modified");
            return list;
        }
        boolean permut;
        do {
            permut = false;
            for (int i = 0; i < length - 1; i++) {
                if (enemy_dist[i].value < enemy_dist[i + 1].value) {
                    Index_value a = enemy_dist[i];
                    enemy_dist[i] = enemy_dist[i + 1];
                    enemy_dist[i + 1] = a;
                    permut = true;
                }
            }
        } while (permut);
        RobotList<Enemy> new_enemy_list = new RobotList<Enemy>(Enemy.class);
        for (int i = 0; i < length; i++) {
            new_enemy_list.addLast(list.get(enemy_dist[i].index));
        }
        return new_enemy_list;
    }

    public RobotList<Resource> sort_incr_Resource(RobotList<Resource> list, String field) {
        int length = list.size();
        Index_value[] resource_dist = new Index_value[length];
        if (field.equals("") || field.equals("location")) {
            Location cur_loc = this.getLocation();
            for (int i = 0; i < length; i++) {
                resource_dist[i] = new Index_value(i, distance(cur_loc, list.get(i).location));
            }
        } else if (field.equals("energy")) {
            for (int i = 0; i < length; i++) {
                resource_dist[i] = new Index_value(i, list.get(i).energy);
            }
        } else if (field.equals("ammostash")) {
            for (int i = 0; i < length; i++) {
                resource_dist[i] = new Index_value(i, list.get(i).ammostash);
            }
        } else if (field.equals("speed")) {
            for (int i = 0; i < length; i++) {
                resource_dist[i] = new Index_value(i, list.get(i).speed);
            }
        } else if (field.equals("health")) {
            for (int i = 0; i < length; i++) {
                resource_dist[i] = new Index_value(i, list.get(i).health);
            }
        } else {
            say("impossible to sort list - nothing modified");
            return list;
        }
        boolean permut;
        do {
            permut = false;
            for (int i = 0; i < length - 1; i++) {
                if (resource_dist[i].value > resource_dist[i + 1].value) {
                    Index_value a = resource_dist[i];
                    resource_dist[i] = resource_dist[i + 1];
                    resource_dist[i + 1] = a;
                    permut = true;
                }
            }
        } while (permut);
        RobotList<Resource> new_resource_list = new RobotList<Resource>(Resource.class);
        for (int i = 0; i < length; i++) {
            new_resource_list.addLast(list.get(resource_dist[i].index));
        }
        return new_resource_list;
    }

    public RobotList<Resource> sort_decr_Resource(RobotList<Resource> list, String field) {
        int length = list.size();
        Index_value[] resource_dist = new Index_value[length];
        if (field.equals("") || field.equals("location")) {
            Location cur_loc = this.getLocation();
            for (int i = 0; i < length; i++) {
                resource_dist[i] = new Index_value(i, distance(cur_loc, list.get(i).location));
            }
        } else if (field.equals("energy")) {
            for (int i = 0; i < length; i++) {
                resource_dist[i] = new Index_value(i, list.get(i).energy);
            }
        } else if (field.equals("ammostash")) {
            for (int i = 0; i < length; i++) {
                resource_dist[i] = new Index_value(i, list.get(i).ammostash);
            }
        } else if (field.equals("speed")) {
            for (int i = 0; i < length; i++) {
                resource_dist[i] = new Index_value(i, list.get(i).speed);
            }
        } else if (field.equals("health")) {
            for (int i = 0; i < length; i++) {
                resource_dist[i] = new Index_value(i, list.get(i).health);
            }
        } else {
            say("impossible to sort list - nothing modified");
            return list;
        }
        boolean permut;
        do {
            permut = false;
            for (int i = 0; i < length - 1; i++) {
                if (resource_dist[i].value < resource_dist[i + 1].value) {
                    Index_value a = resource_dist[i];
                    resource_dist[i] = resource_dist[i + 1];
                    resource_dist[i + 1] = a;
                    permut = true;
                }
            }
        } while (permut);
        RobotList<Resource> new_resource_list = new RobotList<Resource>(Resource.class);
        for (int i = 0; i < length; i++) {
            new_resource_list.addLast(list.get(resource_dist[i].index));
        }
        return new_resource_list;
    }

    public RobotList<Percentage> sort_incr_Percentage(RobotList<Percentage> list, String field) {
        int length = list.size();
        Index_value[] distri = new Index_value[length];
        for (int i = 0; i < length; i++) {
            distri[i] = new Index_value(i, list.get(i).percent);
        }
        boolean permut;
        do {
            permut = false;
            for (int i = 0; i < length - 1; i++) {
                if (distri[i].value > distri[i + 1].value) {
                    Index_value a = distri[i];
                    distri[i] = distri[i + 1];
                    distri[i + 1] = a;
                    permut = true;
                }
            }
        } while (permut);
        RobotList<Percentage> sol = new RobotList<Percentage>(Percentage.class);
        for (int i = 0; i < length; i++) {
            sol.addLast(new Percentage(distri[i].value));
        }
        return sol;
    }

    public RobotList<Percentage> sort_decr_Percentage(RobotList<Percentage> list, String field) {
        int length = list.size();
        Index_value[] distri = new Index_value[length];
        for (int i = 0; i < length; i++) {
            distri[i] = new Index_value(i, list.get(i).percent);
        }
        boolean permut;
        do {
            permut = false;
            for (int i = 0; i < length - 1; i++) {
                if (distri[i].value < distri[i + 1].value) {
                    Index_value a = distri[i];
                    distri[i] = distri[i + 1];
                    distri[i + 1] = a;
                    permut = true;
                }
            }
        } while (permut);
        RobotList<Percentage> sol = new RobotList<Percentage>(Percentage.class);
        for (int i = 0; i < length; i++) {
            sol.addLast(new Percentage(distri[i].value));
        }
        return sol;
    }

    public RobotList<Location> sort_incr_Location(RobotList<Location> list, String field) {
        int length = list.size();
        Index_value[] enemy_dist = new Index_value[length];
        Location cur_loc = this.getLocation();
        for (int i = 0; i < length; i++) {
            enemy_dist[i] = new Index_value(i, distance(cur_loc, list.get(i)));
        }
        boolean permut;
        do {
            permut = false;
            for (int i = 0; i < length - 1; i++) {
                if (enemy_dist[i].value > enemy_dist[i + 1].value) {
                    Index_value a = enemy_dist[i];
                    enemy_dist[i] = enemy_dist[i + 1];
                    enemy_dist[i + 1] = a;
                    permut = true;
                }
            }
        } while (permut);
        RobotList<Location> new_location_list = new RobotList<Location>(Location.class);
        for (int i = 0; i < length; i++) {
            new_location_list.addLast(list.get(enemy_dist[i].index));
        }
        return new_location_list;
    }

    public RobotList<Location> sort_decr_Location(RobotList<Location> list, String field) {
        int length = list.size();
        Index_value[] enemy_dist = new Index_value[length];
        if (field.equals("") || field.equals("location")) {
            Location cur_loc = this.getLocation();
            for (int i = 0; i < length; i++) {
                enemy_dist[i] = new Index_value(i, distance(cur_loc, list.get(i)));
            }
        } else if (field.equals("x")) {
            for (int i = 0; i < length; i++) {
                enemy_dist[i] = new Index_value(i, list.get(i).x);
            }
        } else if (field.equals("y")) {
            for (int i = 0; i < length; i++) {
                enemy_dist[i] = new Index_value(i, list.get(i).y);
            }
        } else {
            say("impossible to sort list - nothing modified");
            return list;
        }
        boolean permut;
        do {
            permut = false;
            for (int i = 0; i < length - 1; i++) {
                if (enemy_dist[i].value < enemy_dist[i + 1].value) {
                    Index_value a = enemy_dist[i];
                    enemy_dist[i] = enemy_dist[i + 1];
                    enemy_dist[i + 1] = a;
                    permut = true;
                }
            }
        } while (permut);
        RobotList<Location> new_location_list = new RobotList<Location>(Location.class);
        for (int i = 0; i < length; i++) {
            new_location_list.addLast(list.get(enemy_dist[i].index));
        }
        return new_location_list;
    }

    public void revert_Enemy() {
        RobotList<Enemy> sol = new RobotList<Enemy>(Enemy.class);
        for (int i = 0; i < enemy_list.size(); i++) {
            sol.addLast(enemy_list.removeLast());
        }
        enemy_list = sol;
    }

    public void revert_Resource() {
        RobotList<Resource> sol = new RobotList<Resource>(Resource.class);
        for (int i = 0; i < resource_list.size(); i++) {
            sol.addLast(resource_list.removeLast());
        }
        resource_list = sol;
    }

    public Enemy projectOnEnemyClass() {
        return new Enemy();
    }

    public void modify_list(RobotList list, String action, Object e, float pos) {
        if (action.equals("add")) {
            list.add(pos, e);
        } else if (action.equals("change")) {
            list.set(pos, e);
        }
    }

    public void modify_list(RobotList list, String action, Object e) {
        if (action.equals("add")) {
            list.add(e);
        } else if (action.equals("remove")) {
            list.removeFirstOccurrence(e);
        }
    }

    public void modify_list(RobotList list, String action, String action2, Float pos) {
        if (action.equals("remove") && action2.equals("item_at")) {
            list.remove(pos);
        }
    }

    public Percentage gen_random_num() {
        return new Percentage((float) Math.random() * 100);
    }

    public boolean flip_coin(Float s) {
        return ((float) Math.random() < s);
    }

    public void setPosition(float x, float y, float z) {
        position = new Vector3(x, y, z);
    }

    public Location getLocation() {
        return new Location(position.x, position.z);
    }

    public Location getlocation() {
        return new Location(position.x, position.z);
    }

    public Location get_location() {
        return new Location(position.x, position.z);
    }
}
