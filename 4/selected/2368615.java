package org.photovault.swingui;

import org.odmg.LockNotGrantedException;
import org.photovault.common.PhotovaultException;
import org.photovault.dbhelper.ODMGXAWrapper;
import org.photovault.dcraw.ColorProfileDesc;
import org.photovault.dcraw.RawConversionSettings;
import org.photovault.dcraw.RawSettingsFactory;
import org.photovault.image.ChannelMapOperation;
import org.photovault.image.ChannelMapOperationFactory;
import org.photovault.image.ColorCurve;
import org.photovault.image.PhotovaultImage;
import org.photovault.imginfo.FuzzyDate;
import java.util.*;
import java.io.*;
import org.photovault.imginfo.*;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoNotFoundException;
import org.photovault.swingui.folderpane.FolderController;
import org.apache.log4j.Logger;

/**
 PhotoInfoController contains the application logic for creating and editing 
 PhotoInfo records in database, i.e. it implements the controller role in MVC 
 pattern.
 */
public class PhotoInfoController {

    static Logger log = Logger.getLogger(PhotoInfoController.class.getName());

    /**
     Default constructor
     */
    public PhotoInfoController() {
        modelFields = new HashMap();
        views = new Vector();
        initModelFields();
    }

    /**
     Special field controller for handling UI fields that are stored as part of
     RawConversionSettings.
     */
    abstract class RawSettingFieldCtrl extends FieldController {

        /**
         * Constructs a new RawSettingsFieldCtrl
         * @param model PhotoInfo objects that form the model
         * @param field Field that is controlled bu this object
         */
        public RawSettingFieldCtrl(Object model, String field) {
            super(model);
            this.field = field;
        }

        final String field;

        /**
         * This must be overridden by derived classes to set the raw settings field in the
         * RawSettingsFactory that will be used for creating new settings for controlled 
         * object.
         * @param f The factory in which set field must be set
         * @param newValue New value for the field. Note that this can be <code>null</code> even for 
         * primitive type fields.
         */
        protected abstract void doSetModelValue(RawSettingsFactory f, Object newValue);

        /**
         * Get the value of the controlled field in RawConversionSettings.
         * @param r The raw conversion settings object whose field value we are interested in.
         * @return Value of field in r
         */
        protected abstract Object doGetModelValue(RawConversionSettings r);

        /**
         * Set a view to reflect current model value
         * @param view The view that should be set up.
         */
        protected abstract void doSetViewValue(RawPhotoView view);

        /**
         * Get the value of this field in a given view
         * @param view The view whose field value must be retrieved
         * @return Value of controlled field in given view.
         */
        protected abstract Object doGetViewValue(RawPhotoView view);

        /**
         * Set the multivalued state in a given view
         * @param view The view that will be set up.
         */
        protected abstract void doSetViewMultivaluedState(RawPhotoView view);

        protected void setModelValue(Object model) {
            PhotoInfo obj = (PhotoInfo) model;
            RawSettingsFactory f = getRawSettingsFactory(obj);
            if (f != null) {
                doSetModelValue(f, value);
                rawSettingsChanged();
            }
        }

        protected Object getModelValue(Object model) {
            PhotoInfo obj = (PhotoInfo) model;
            RawConversionSettings r = obj.getRawSettings();
            Object ret = null;
            if (r != null) {
                ret = doGetModelValue(r);
            }
            return ret;
        }

        protected void updateView(Object view) {
            if (view instanceof RawPhotoView) {
                RawPhotoView obj = (RawPhotoView) view;
                doSetViewValue((RawPhotoView) view);
            }
        }

        protected void updateViewMultivalueState(Object view) {
            if (view instanceof RawPhotoView) {
                RawPhotoView obj = (RawPhotoView) view;
                doSetViewMultivaluedState(obj);
            }
        }

        protected void updateValue(Object view) {
            if (view instanceof RawPhotoView) {
                RawPhotoView obj = (RawPhotoView) view;
                value = doGetViewValue(obj);
            }
        }
    }

    ;

    /**
     Special field controller for handling Color curves.
     */
    class ColorCurveCtrl extends FieldController {

        /**
         * Constructs a new ColorCurveCtrl
         * @param model PhotoInfo objects that form the model
         * @param curveName Name of the generated curve
         */
        public ColorCurveCtrl(Object model, String curveName) {
            super(model);
            this.name = curveName;
        }

        final String name;

        protected void setModelValue(Object model) {
            PhotoInfo obj = (PhotoInfo) model;
            ChannelMapOperationFactory f = getColorMappingFactory(obj);
            if (f != null) {
                f.setChannelCurve(name, (ColorCurve) value);
            }
            colorMappingChanged();
        }

        protected Object getModelValue(Object model) {
            PhotoInfo obj = (PhotoInfo) model;
            ChannelMapOperation cm = obj.getColorChannelMapping();
            ColorCurve ret = null;
            if (cm != null) {
                ret = cm.getChannelCurve(name);
            }
            return ret;
        }

        protected void updateView(Object view) {
            if (view instanceof PhotoInfoView) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setColorChannelCurve(name, (ColorCurve) value);
            }
        }

        protected void updateViewMultivalueState(Object view) {
            if (view instanceof PhotoInfoView) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setColorChannelMultivalued(name, isMultiValued, (valueSet != null) ? (ColorCurve[]) valueSet.toArray(new ColorCurve[0]) : null);
            }
        }

        protected void updateValue(Object view) {
            if (view instanceof PhotoInfoView) {
                PhotoInfoView obj = (PhotoInfoView) view;
                value = obj.getColorChannelCurve(name);
            }
        }
    }

    ;

    /**
     initModelFields() initializes the modelFields structure to match the model object.
     It will contain one FieldController object for each fields in the model.
     */
    protected void initModelFields() {
        modelFields.put(PHOTOGRAPHER, new FieldController(photos) {

            protected void setModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                obj.setPhotographer((String) value);
            }

            protected Object getModelValue(Object model) {
                if (model == null) {
                    return null;
                }
                PhotoInfo obj = (PhotoInfo) model;
                return obj.getPhotographer();
            }

            protected void updateView(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setPhotographer((String) value);
            }

            protected void updateViewMultivalueState(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setPhotographerMultivalued(isMultiValued);
            }

            protected void updateValue(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                value = obj.getPhotographer();
            }
        });
        modelFields.put(FUZZY_DATE, new FieldController(photos) {

            protected void setModelValue(Object model) {
                log.debug("FUZZY_DATE - setModeValue ({}) ");
                PhotoInfo obj = (PhotoInfo) model;
                FuzzyDate fd = (FuzzyDate) value;
                if (fd != null) {
                    obj.setShootTime(fd.getDate());
                    obj.setTimeAccuracy(fd.getAccuracy());
                } else {
                    obj.setShootTime(null);
                    obj.setTimeAccuracy(0);
                }
            }

            protected Object getModelValue(Object model) {
                log.debug("FUZZY_DATE - getModeValue ({}) ");
                PhotoInfo obj = (PhotoInfo) model;
                Date date = obj.getShootTime();
                double accuracy = obj.getTimeAccuracy();
                return new FuzzyDate(date, accuracy);
            }

            protected void updateView(Object view) {
                log.debug("FUZZY_DATE - updateView ({}) ");
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setFuzzyDate((FuzzyDate) value);
            }

            protected void updateViewMultivalueState(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setFuzzyDateMultivalued(isMultiValued);
            }

            protected void updateValue(Object view) {
                log.debug("FUZZY_DATE - updateValue ({}) ");
                PhotoInfoView obj = (PhotoInfoView) view;
                value = obj.getFuzzyDate();
            }
        });
        modelFields.put(QUALITY, new FieldController(photos) {

            protected void setModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                if (value != null) {
                    obj.setQuality(((Number) value).intValue());
                } else {
                    obj.setQuality(0);
                }
            }

            protected Object getModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                return new Double(obj.getQuality());
            }

            protected void updateView(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setQuality((Number) value);
            }

            protected void updateViewMultivalueState(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setQualityMultivalued(isMultiValued);
            }

            protected void updateValue(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                value = obj.getQuality();
            }
        });
        modelFields.put(SHOOTING_PLACE, new FieldController(photos) {

            protected void setModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                obj.setShootingPlace((String) value);
            }

            protected Object getModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                return obj.getShootingPlace();
            }

            protected void updateView(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setShootPlace((String) value);
            }

            protected void updateViewMultivalueState(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setShootPlaceMultivalued(isMultiValued);
            }

            protected void updateValue(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                value = obj.getShootPlace();
            }
        });
        modelFields.put(CAMERA_MODEL, new FieldController(photos) {

            protected void setModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                obj.setCamera((String) value);
            }

            protected Object getModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                return obj.getCamera();
            }

            protected void updateView(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setCamera((String) value);
            }

            protected void updateViewMultivalueState(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setCameraMultivalued(isMultiValued);
            }

            protected void updateValue(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                value = obj.getCamera();
            }
        });
        modelFields.put(FILM_TYPE, new FieldController(photos) {

            protected void setModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                obj.setFilm((String) value);
            }

            protected Object getModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                return obj.getFilm();
            }

            protected void updateView(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setFilm((String) value);
            }

            protected void updateViewMultivalueState(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setFilmMultivalued(isMultiValued);
            }

            protected void updateValue(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                value = obj.getFilm();
            }
        });
        modelFields.put(LENS_TYPE, new FieldController(photos) {

            protected void setModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                obj.setLens((String) value);
            }

            protected Object getModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                return obj.getLens();
            }

            protected void updateView(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setLens((String) value);
            }

            protected void updateViewMultivalueState(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setLensMultivalued(isMultiValued);
            }

            protected void updateValue(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                value = obj.getLens();
            }
        });
        modelFields.put(DESCRIPTION, new FieldController(photos) {

            protected void setModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                obj.setDescription((String) value);
            }

            protected Object getModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                return obj.getDescription();
            }

            protected void updateView(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setDescription((String) value);
            }

            protected void updateViewMultivalueState(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setDescriptionMultivalued(isMultiValued);
            }

            protected void updateValue(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                value = obj.getDescription();
            }
        });
        modelFields.put(TECHNOTE, new FieldController(photos) {

            protected void setModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                obj.setTechNotes((String) value);
            }

            protected Object getModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                return obj.getTechNotes();
            }

            protected void updateView(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setTechNote((String) value);
            }

            protected void updateViewMultivalueState(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setTechNoteMultivalued(isMultiValued);
            }

            protected void updateValue(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                value = obj.getTechNote();
            }
        });
        modelFields.put(F_STOP, new FieldController(photos) {

            protected void setModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                if (value != null) {
                    obj.setFStop(((Number) value).doubleValue());
                } else {
                    obj.setFStop(0);
                }
            }

            protected Object getModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                return new Double(obj.getFStop());
            }

            protected void updateView(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setFStop((Number) value);
            }

            protected void updateViewMultivalueState(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setFStopMultivalued(isMultiValued);
            }

            protected void updateValue(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                value = obj.getFStop();
            }
        });
        modelFields.put(SHUTTER_SPEED, new FieldController(photos) {

            protected void setModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                if (value != null) {
                    obj.setShutterSpeed(((Number) value).doubleValue());
                } else {
                    obj.setShutterSpeed(0);
                }
            }

            protected Object getModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                return new Double(obj.getShutterSpeed());
            }

            protected void updateView(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setShutterSpeed((Number) value);
            }

            protected void updateViewMultivalueState(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setShutterSpeedMultivalued(isMultiValued);
            }

            protected void updateValue(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                value = obj.getShutterSpeed();
            }
        });
        modelFields.put(FOCAL_LENGTH, new FieldController(photos) {

            protected void setModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                if (value != null) {
                    obj.setFocalLength(((Number) value).doubleValue());
                } else {
                    obj.setFocalLength(0);
                }
            }

            protected Object getModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                return new Double(obj.getFocalLength());
            }

            protected void updateView(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setFocalLength((Number) value);
            }

            protected void updateViewMultivalueState(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setFocalLengthMultivalued(isMultiValued);
            }

            protected void updateValue(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                value = obj.getFocalLength();
            }
        });
        modelFields.put(FILM_SPEED, new FieldController(photos) {

            protected void setModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                if (value != null) {
                    obj.setFilmSpeed(((Number) value).intValue());
                } else {
                    obj.setFilmSpeed(0);
                }
            }

            protected Object getModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                return new Double(obj.getFilmSpeed());
            }

            protected void updateView(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setFilmSpeed((Number) value);
            }

            protected void updateViewMultivalueState(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setFilmSpeedMultivalued(isMultiValued);
            }

            protected void updateValue(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                value = obj.getFilmSpeed();
            }
        });
        modelFields.put(RAW_SETTINGS, new FieldController(photos) {

            protected void setModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                obj.setRawSettings((RawConversionSettings) value);
            }

            protected Object getModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                return obj.getRawSettings();
            }

            protected void updateView(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setRawSettings((RawConversionSettings) value);
            }

            protected void updateViewMultivalueState(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setRawSettingsMultivalued(isMultiValued);
            }

            protected void updateValue(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                value = obj.getRawSettings();
            }
        });
        modelFields.put(RAW_BLACK_LEVEL, new RawSettingFieldCtrl(photos, RAW_BLACK_LEVEL) {

            protected void doSetModelValue(RawSettingsFactory f, Object newValue) {
                if (newValue != null) {
                    f.setBlack(((Number) value).intValue());
                } else {
                    f.setBlack(0);
                }
            }

            protected Object doGetModelValue(RawConversionSettings r) {
                return new Integer(r.getBlack());
            }

            protected void doSetViewValue(RawPhotoView view) {
                if (value != null) {
                    view.setRawBlack(((Number) value).intValue());
                } else {
                    view.setRawBlack(0);
                }
            }

            protected Object doGetViewValue(RawPhotoView view) {
                return new Integer(view.getRawBlack());
            }

            protected void doSetViewMultivaluedState(RawPhotoView view) {
                view.setRawBlackMultivalued(isMultiValued, valueSet.toArray());
            }
        });
        modelFields.put(RAW_EV_CORR, new RawSettingFieldCtrl(photos, RAW_EV_CORR) {

            protected void doSetModelValue(RawSettingsFactory f, Object newValue) {
                if (newValue != null) {
                    f.setEvCorr(((Number) value).doubleValue());
                } else {
                    f.setEvCorr(0);
                }
            }

            protected Object doGetModelValue(RawConversionSettings r) {
                return new Double(r.getEvCorr());
            }

            protected void doSetViewValue(RawPhotoView view) {
                if (value != null) {
                    view.setRawEvCorr(((Number) value).doubleValue());
                } else {
                    view.setRawEvCorr(0.0);
                }
            }

            protected Object doGetViewValue(RawPhotoView view) {
                return new Double(view.getRawEvCorr());
            }

            protected void doSetViewMultivaluedState(RawPhotoView view) {
                view.setRawEvCorrMultivalued(this.isMultiValued, valueSet.toArray());
            }
        });
        modelFields.put(RAW_HLIGHT_COMP, new RawSettingFieldCtrl(photos, RAW_HLIGHT_COMP) {

            protected void doSetModelValue(RawSettingsFactory f, Object newValue) {
                if (newValue != null) {
                    f.setHlightComp(((Number) value).doubleValue());
                } else {
                    f.setHlightComp(0);
                }
            }

            protected Object doGetModelValue(RawConversionSettings r) {
                return new Double(r.getHighlightCompression());
            }

            protected void doSetViewValue(RawPhotoView view) {
                if (value != null) {
                    view.setRawHlightComp(((Number) value).doubleValue());
                } else {
                    view.setRawHlightComp(0.0);
                }
            }

            protected Object doGetViewValue(RawPhotoView view) {
                return new Double(view.getRawHlightComp());
            }

            protected void doSetViewMultivaluedState(RawPhotoView view) {
                view.setRawHlightCompMultivalued(this.isMultiValued, valueSet.toArray());
            }
        });
        modelFields.put(RAW_CTEMP, new RawSettingFieldCtrl(photos, RAW_CTEMP) {

            protected void doSetModelValue(RawSettingsFactory f, Object newValue) {
                if (newValue != null) {
                    f.setColorTemp(((Number) value).doubleValue());
                } else {
                    f.setColorTemp(0);
                }
            }

            protected Object doGetModelValue(RawConversionSettings r) {
                return new Double(r.getColorTemp());
            }

            protected void doSetViewValue(RawPhotoView view) {
                if (value != null) {
                    view.setRawColorTemp(((Number) value).doubleValue());
                } else {
                    view.setRawColorTemp(0.0);
                }
            }

            protected Object doGetViewValue(RawPhotoView view) {
                return new Double(view.getRawColorTemp());
            }

            protected void doSetViewMultivaluedState(RawPhotoView view) {
                view.setRawColorTempMultivalued(this.isMultiValued, valueSet.toArray());
            }
        });
        modelFields.put(RAW_GREEN, new RawSettingFieldCtrl(photos, RAW_GREEN) {

            protected void doSetModelValue(RawSettingsFactory f, Object newValue) {
                if (newValue != null) {
                    f.setGreenGain(((Number) value).doubleValue());
                } else {
                    f.setGreenGain(0);
                }
            }

            protected Object doGetModelValue(RawConversionSettings r) {
                return new Double(r.getGreenGain());
            }

            protected void doSetViewValue(RawPhotoView view) {
                if (value != null) {
                    view.setRawGreenGain(((Number) value).doubleValue());
                } else {
                    view.setRawGreenGain(0.0);
                }
            }

            protected Object doGetViewValue(RawPhotoView view) {
                return new Double(view.getRawGreenGain());
            }

            protected void doSetViewMultivaluedState(RawPhotoView view) {
                view.setRawGreenGainMultivalued(this.isMultiValued, valueSet.toArray());
            }
        });
        modelFields.put(RAW_COLOR_PROFILE, new RawSettingFieldCtrl(photos, RAW_COLOR_PROFILE) {

            protected void doSetModelValue(RawSettingsFactory f, Object newValue) {
                f.setColorProfile((ColorProfileDesc) newValue);
            }

            protected Object doGetModelValue(RawConversionSettings r) {
                return r.getColorProfile();
            }

            protected void doSetViewValue(RawPhotoView view) {
                view.setRawProfile((ColorProfileDesc) value);
            }

            protected Object doGetViewValue(RawPhotoView view) {
                return view.getRawProfile();
            }

            protected void doSetViewMultivaluedState(RawPhotoView view) {
                view.setRawProfileMultivalued(this.isMultiValued, valueSet.toArray());
            }
        });
        modelFields.put(COLOR_MAPPING, new FieldController(photos) {

            protected void setModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                obj.setColorChannelMapping((ChannelMapOperation) value);
            }

            protected Object getModelValue(Object model) {
                PhotoInfo obj = (PhotoInfo) model;
                return obj.getColorChannelMapping();
            }

            protected void updateView(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setColorChannelMapping((ChannelMapOperation) value);
            }

            protected void updateViewMultivalueState(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setColorChannelMappingMultivalued(isMultiValued);
            }

            protected void updateValue(Object view) {
                PhotoInfoView obj = (PhotoInfoView) view;
                value = obj.getColorChannelMapping();
            }
        });
        modelFields.put(PREVIEW_IMAGE, new FieldController(photos) {

            protected void setModelValue(Object model) {
            }

            protected Object getModelValue(Object model) {
                return null;
            }

            protected void updateView(Object view) {
                if (view instanceof PreviewImageView) {
                    PreviewImageView obj = (PreviewImageView) view;
                    obj.modelPreviewImageChanged((PhotovaultImage) value);
                }
            }

            protected void updateViewMultivalueState(Object view) {
            }

            protected void updateValue(Object view) {
                if (view instanceof PreviewImageView) {
                    PreviewImageView obj = (PreviewImageView) view;
                    value = obj.getPreviewImage();
                } else {
                    value = null;
                }
            }
        });
        modelFields.put(COLOR_CURVE_VALUE, new ColorCurveCtrl(photos, "value"));
        modelFields.put(COLOR_CURVE_RED, new ColorCurveCtrl(photos, "red"));
        modelFields.put(COLOR_CURVE_GREEN, new ColorCurveCtrl(photos, "green"));
        modelFields.put(COLOR_CURVE_BLUE, new ColorCurveCtrl(photos, "blue"));
        modelFields.put(COLOR_CURVE_SATURATION, new ColorCurveCtrl(photos, "saturation"));
        folderCtrl = new FolderController(photos);
        modelFields.put(PHOTO_FOLDERS, folderCtrl);
        Iterator iter = modelFields.values().iterator();
        while (iter.hasNext()) {
            FieldController fieldCtrl = (FieldController) iter.next();
            fieldCtrl.setViews(views);
        }
    }

    protected PhotoInfo[] photos = null;

    protected boolean isCreatingNew = true;

    protected Collection views = null;

    /**
     Sets the PhotoInfo record that will be edited
     @param photo The photoInfo object that is to be edited. If null the a new PhotoInfo record will be created
     */
    public void setPhoto(PhotoInfo photo) {
        if (photo != null) {
            isCreatingNew = false;
        } else {
            isCreatingNew = true;
        }
        this.photos = new PhotoInfo[1];
        photos[0] = photo;
        changeModelInFields(isCreatingNew);
    }

    /**
     Sets a group of PhotoInfo records that will be edited. If all of the records will have same value for a
     certain field the views will display this value. Otherwise, <code>null</code> is displayed and if the
     value is changed in a view, the new value is updated to all controlled objects.
     */
    public void setPhotos(PhotoInfo[] photos) {
        this.photos = photos;
        isCreatingNew = false;
        changeModelInFields(isCreatingNew);
    }

    /**
     Sets the view that is contorlled by this object
     @param view The controlled view
     */
    public void setView(PhotoInfoView view) {
        views.clear();
        addView(view);
    }

    /**
     Add a new view to those that are controlled by this object.
     TODO: Only the new view should be set to match the model.
     @param view The view to add.
     */
    public void addView(PhotoInfoView view) {
        views.add(view);
        Iterator iter = modelFields.values().iterator();
        while (iter.hasNext()) {
            FieldController fieldCtrl = (FieldController) iter.next();
            fieldCtrl.updateAllViews();
        }
    }

    protected void changeModelInFields(boolean preserveFieldState) {
        Iterator fieldIter = modelFields.values().iterator();
        while (fieldIter.hasNext()) {
            FieldController fieldCtrl = (FieldController) fieldIter.next();
            fieldCtrl.setModel(photos, preserveFieldState);
        }
        rawFactories.clear();
        colorMappingFactories.clear();
    }

    /**
     Sets up the controller to create a new PhotoInfo
     @param imageFile the original image that is to be added to database
     */
    public void createNewPhoto(File imageFile) {
        setPhoto(null);
        originalFile = imageFile;
        isCreatingNew = true;
    }

    /**
     Returns the hotoInfo record that is currently edited.
     */
    public PhotoInfo getPhoto() {
        PhotoInfo photo = null;
        if (photos != null) {
            photo = photos[0];
        }
        return photo;
    }

    /**
     Get the photos in current model.
     */
    public PhotoInfo[] getPhotos() {
        if (photos != null) {
            return photos.clone();
        }
        return null;
    }

    public FolderController getFolderController() {
        return folderCtrl;
    }

    FolderController folderCtrl;

    /**
     Save the modifications made to the PhotoInfo record
     @throws PhotovaultException if an error occurs during save (most likely 
     the photo was locked by another operation. In this case the transaction 
     is canceled.
     @throws PhotoNotFoundException if the original image cound not be located
     */
    public void save() throws PhotoNotFoundException, PhotovaultException {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        try {
            if (isCreatingNew) {
                photos = new PhotoInfo[1];
                if (originalFile != null) {
                    photos[0] = PhotoInfo.addToDB(originalFile);
                } else {
                    photos[0] = PhotoInfo.create();
                }
                changeModelInFields(true);
                isCreatingNew = false;
            }
            Iterator fieldIter = modelFields.values().iterator();
            while (fieldIter.hasNext()) {
                FieldController fieldCtrl = (FieldController) fieldIter.next();
                fieldCtrl.save();
            }
            if (isRawSettingsChanged) {
                Iterator rawIter = rawFactories.entrySet().iterator();
                while (rawIter.hasNext()) {
                    Map.Entry e = (Map.Entry) rawIter.next();
                    PhotoInfo p = (PhotoInfo) e.getKey();
                    RawSettingsFactory f = (RawSettingsFactory) e.getValue();
                    RawConversionSettings r = null;
                    try {
                        r = f.create();
                    } catch (PhotovaultException ex) {
                        ex.printStackTrace();
                    }
                    p.setRawSettings(r);
                }
            }
            if (isColorMappingChanged) {
                Iterator colorIter = colorMappingFactories.entrySet().iterator();
                while (colorIter.hasNext()) {
                    Map.Entry e = (Map.Entry) colorIter.next();
                    PhotoInfo p = (PhotoInfo) e.getKey();
                    ChannelMapOperationFactory f = (ChannelMapOperationFactory) e.getValue();
                    ChannelMapOperation o = null;
                    o = f.create();
                    p.setColorChannelMapping(o);
                }
            }
        } catch (LockNotGrantedException e) {
            txw.abort();
            throw new PhotovaultException("Photo locked for other use", e);
        }
        txw.commit();
    }

    /**
     Discards modifications done after last save
     */
    public void discard() {
        Iterator fieldIter = modelFields.values().iterator();
        while (fieldIter.hasNext()) {
            FieldController fieldCtrl = (FieldController) fieldIter.next();
            fieldCtrl.setModel(photos, false);
        }
        rawFactories.clear();
        colorMappingFactories.clear();
    }

    /**
     Adds a new listener that will be notified of events related to this object
     */
    public void addListener(PhotoInfoListener l) {
    }

    public static final String PHOTOGRAPHER = "Photographer";

    public static final String FUZZY_DATE = "Fuzzy date";

    public static final String QUALITY = "Quality";

    public static final String SHOOTING_PLACE = "Shooting place";

    public static final String DESCRIPTION = "Description";

    public static final String TECHNOTE = "Tech note";

    public static final String F_STOP = "F-stop";

    public static final String SHUTTER_SPEED = "Shutter speed";

    public static final String FOCAL_LENGTH = "Focal length";

    public static final String CAMERA_MODEL = "Camera model";

    public static final String FILM_TYPE = "Film type";

    public static final String FILM_SPEED = "Film speed";

    public static final String LENS_TYPE = "Lens type";

    public static final String PHOTO_FOLDERS = "Photo folders";

    public static final String RAW_SETTINGS = "Raw conversion settings";

    public static final String RAW_BLACK_LEVEL = "Raw conversion black level";

    public static final String RAW_EV_CORR = "Raw conversion EV correction";

    public static final String RAW_HLIGHT_COMP = "Raw conversion highlight compression";

    public static final String RAW_CTEMP = "Raw conversion color temperature";

    public static final String RAW_GREEN = "Raw conversion green gain";

    public static final String RAW_COLOR_PROFILE = "Raw conversion ICC profile";

    public static final String COLOR_MAPPING = "Color channel mapping";

    public static final String COLOR_CURVE_VALUE = "Value color curve";

    public static final String COLOR_CURVE_RED = "Red color curve";

    public static final String COLOR_CURVE_GREEN = "Green color curve";

    public static final String COLOR_CURVE_BLUE = "Blue color curve";

    public static final String COLOR_CURVE_SATURATION = "Saturation curve";

    public static final String PREVIEW_IMAGE = "Preview image";

    protected HashMap modelFields = null;

    File originalFile = null;

    public void setField(String field, Object value) {
        FieldController fieldCtrl = (FieldController) modelFields.get(field);
        if (fieldCtrl != null) {
            fieldCtrl.setValue(value);
        } else {
            log.warn("No field " + field);
        }
    }

    /**
     This method must be called by a view when it has been changed
     @param view The changed view
     @param field The field that has been changed
     @param newValue New value for the field
     @deprecated Use viewChanged( view, field ) instead.
     */
    public void viewChanged(PhotoInfoView view, String field, Object newValue) {
        FieldController fieldCtrl = (FieldController) modelFields.get(field);
        if (fieldCtrl != null) {
            fieldCtrl.viewChanged(view, newValue);
        } else {
            log.warn("No field " + field);
        }
    }

    /**
     This method must be called by a view when it has been changed
     @param view The changed view
     @param field The field that has been changed
     */
    public void viewChanged(PhotoInfoView view, String field) {
        FieldController fieldCtrl = (FieldController) modelFields.get(field);
        if (fieldCtrl != null) {
            fieldCtrl.viewChanged(view);
        } else {
            log.warn("No field " + field);
        }
    }

    /**
     Returns the current value for a specified field
     @param field The field whose value is to be retrieved
     @return Value of the field or null if fiels is invalid
     */
    public Object getField(String field) {
        Object value = null;
        FieldController fieldCtrl = (FieldController) modelFields.get(field);
        if (fieldCtrl != null) {
            value = fieldCtrl.getValue();
        }
        return value;
    }

    HashMap rawFactories = new HashMap();

    private RawSettingsFactory getRawSettingsFactory(PhotoInfo p) {
        RawSettingsFactory f = null;
        if (rawFactories.containsKey(p)) {
            f = (RawSettingsFactory) rawFactories.get(p);
        } else {
            RawConversionSettings r = p.getRawSettings();
            f = new RawSettingsFactory(r);
            if (r != null) {
                rawFactories.put(p, f);
            }
        }
        return f;
    }

    HashMap colorMappingFactories = new HashMap();

    private ChannelMapOperationFactory getColorMappingFactory(PhotoInfo p) {
        ChannelMapOperationFactory f = null;
        if (colorMappingFactories.containsKey(p)) {
            f = (ChannelMapOperationFactory) colorMappingFactories.get(p);
        } else {
            ChannelMapOperation r = p.getColorChannelMapping();
            f = new ChannelMapOperationFactory(r);
            colorMappingFactories.put(p, f);
        }
        return f;
    }

    boolean isRawSettingsChanged = false;

    private void rawSettingsChanged() {
        isRawSettingsChanged = true;
    }

    boolean isColorMappingChanged = false;

    private void colorMappingChanged() {
        isColorMappingChanged = true;
    }
}
