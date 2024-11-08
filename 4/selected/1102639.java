package ch.hsr.orm.model.diagram.providers;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gmf.runtime.common.core.service.AbstractProvider;
import org.eclipse.gmf.runtime.common.core.service.IOperation;
import org.eclipse.gmf.runtime.common.ui.services.parser.GetParserOperation;
import org.eclipse.gmf.runtime.common.ui.services.parser.IParser;
import org.eclipse.gmf.runtime.common.ui.services.parser.IParserProvider;
import org.eclipse.gmf.runtime.emf.type.core.IElementType;
import org.eclipse.gmf.runtime.emf.ui.services.parser.ParserHintAdapter;
import org.eclipse.gmf.runtime.notation.View;
import ch.hsr.orm.model.ModelPackage;
import ch.hsr.orm.model.diagram.edit.parts.Attribute2EditPart;
import ch.hsr.orm.model.diagram.edit.parts.AttributeEditPart;
import ch.hsr.orm.model.diagram.edit.parts.BiManyToManyOwnedAttributeEditPart;
import ch.hsr.orm.model.diagram.edit.parts.BiManyToManyOwnerAttributeEditPart;
import ch.hsr.orm.model.diagram.edit.parts.BiOneToManyOwnedAttributeEditPart;
import ch.hsr.orm.model.diagram.edit.parts.BiOneToManyOwnerAttributeEditPart;
import ch.hsr.orm.model.diagram.edit.parts.BiOneToOneOwnedAttributeEditPart;
import ch.hsr.orm.model.diagram.edit.parts.BiOneToOneOwnerAttributeEditPart;
import ch.hsr.orm.model.diagram.edit.parts.CompositionAttributeNameEditPart;
import ch.hsr.orm.model.diagram.edit.parts.CompositionPrimaryKeyEditPart;
import ch.hsr.orm.model.diagram.edit.parts.EmbeddedNameEditPart;
import ch.hsr.orm.model.diagram.edit.parts.EntityNameEditPart;
import ch.hsr.orm.model.diagram.edit.parts.SequenceGeneratorSequenceNameEditPart;
import ch.hsr.orm.model.diagram.edit.parts.TableGeneratorTableNameEditPart;
import ch.hsr.orm.model.diagram.edit.parts.UniManyToManyOwnerAttributeEditPart;
import ch.hsr.orm.model.diagram.edit.parts.UniOneToManyOwnerAttributeEditPart;
import ch.hsr.orm.model.diagram.edit.parts.UniOneToOneOwnerAttributeEditPart;
import ch.hsr.orm.model.diagram.edit.parts.VersionAttributeEditPart;
import ch.hsr.orm.model.diagram.parsers.CompositeParser;
import ch.hsr.orm.model.diagram.parsers.MessageFormatParser;
import ch.hsr.orm.model.diagram.parsers.RegexpParser;
import ch.hsr.orm.model.diagram.part.OrmmetaVisualIDRegistry;

/**
 * @generated
 */
public class OrmmetaParserProvider extends AbstractProvider implements IParserProvider {

    /**
	 * @generated
	 */
    private IParser entityName_4001Parser;

    /**
	 * @generated
	 */
    private IParser getEntityName_4001Parser() {
        if (entityName_4001Parser == null) {
            entityName_4001Parser = createEntityName_4001Parser();
        }
        return entityName_4001Parser;
    }

    /**
	 * @generated
	 */
    protected IParser createEntityName_4001Parser() {
        EAttribute[] features = new EAttribute[] { ModelPackage.eINSTANCE.getPersistable_Name() };
        MessageFormatParser parser = new MessageFormatParser(features);
        return parser;
    }

    /**
	 * @generated
	 */
    private IParser embeddedName_4003Parser;

    /**
	 * @generated
	 */
    private IParser getEmbeddedName_4003Parser() {
        if (embeddedName_4003Parser == null) {
            embeddedName_4003Parser = createEmbeddedName_4003Parser();
        }
        return embeddedName_4003Parser;
    }

    /**
	 * @generated
	 */
    protected IParser createEmbeddedName_4003Parser() {
        EAttribute[] features = new EAttribute[] { ModelPackage.eINSTANCE.getPersistable_Name() };
        MessageFormatParser parser = new MessageFormatParser(features);
        return parser;
    }

    /**
	 * @generated
	 */
    private IParser tableGeneratorTableName_4006Parser;

    /**
	 * @generated
	 */
    private IParser getTableGeneratorTableName_4006Parser() {
        if (tableGeneratorTableName_4006Parser == null) {
            tableGeneratorTableName_4006Parser = createTableGeneratorTableName_4006Parser();
        }
        return tableGeneratorTableName_4006Parser;
    }

    /**
	 * @generated
	 */
    protected IParser createTableGeneratorTableName_4006Parser() {
        EAttribute[] features = new EAttribute[] { ModelPackage.eINSTANCE.getTableGenerator_TableName() };
        MessageFormatParser parser = new MessageFormatParser(features);
        return parser;
    }

    /**
	 * @generated
	 */
    private IParser sequenceGeneratorSequenceName_4008Parser;

    /**
	 * @generated
	 */
    private IParser getSequenceGeneratorSequenceName_4008Parser() {
        if (sequenceGeneratorSequenceName_4008Parser == null) {
            sequenceGeneratorSequenceName_4008Parser = createSequenceGeneratorSequenceName_4008Parser();
        }
        return sequenceGeneratorSequenceName_4008Parser;
    }

    /**
	 * @generated
	 */
    protected IParser createSequenceGeneratorSequenceName_4008Parser() {
        EAttribute[] features = new EAttribute[] { ModelPackage.eINSTANCE.getSequenceGenerator_SequenceName() };
        MessageFormatParser parser = new MessageFormatParser(features);
        return parser;
    }

    /**
	 * @generated
	 */
    private IParser attribute_2001Parser;

    /**
	 * @generated
	 */
    private IParser getAttribute_2001Parser() {
        if (attribute_2001Parser == null) {
            attribute_2001Parser = createAttribute_2001Parser();
        }
        return attribute_2001Parser;
    }

    /**
	 * @generated
	 */
    protected IParser createAttribute_2001Parser() {
        EAttribute[] features = new EAttribute[] { ModelPackage.eINSTANCE.getAttribute_Name(), ModelPackage.eINSTANCE.getAttribute_Datatype(), ModelPackage.eINSTANCE.getAttribute_PrimaryKey() };
        MessageFormatParser reader = new MessageFormatParser(features);
        reader.setViewPattern("{0} : {1} {2,choice,0#|1#'<<'id'>>'}");
        reader.setEditorPattern("{0} : {1}");
        RegexpParser writer = new RegexpParser(features);
        writer.setEditPattern("{0} *: *{1}");
        return new CompositeParser(reader, writer);
    }

    /**
	 * @generated
	 */
    private IParser versionAttribute_2002Parser;

    /**
	 * @generated
	 */
    private IParser getVersionAttribute_2002Parser() {
        if (versionAttribute_2002Parser == null) {
            versionAttribute_2002Parser = createVersionAttribute_2002Parser();
        }
        return versionAttribute_2002Parser;
    }

    /**
	 * @generated
	 */
    protected IParser createVersionAttribute_2002Parser() {
        EAttribute[] features = new EAttribute[] { ModelPackage.eINSTANCE.getVersionAttribute_Name(), ModelPackage.eINSTANCE.getVersionAttribute_Datatype() };
        MessageFormatParser reader = new MessageFormatParser(features);
        reader.setViewPattern("{0} : {1} <<version>>");
        reader.setEditorPattern("{0} : {1}");
        RegexpParser writer = new RegexpParser(features);
        writer.setEditPattern("{0} *: *{1}");
        return new CompositeParser(reader, writer);
    }

    /**
	 * @generated
	 */
    private IParser attribute_2003Parser;

    /**
	 * @generated
	 */
    private IParser getAttribute_2003Parser() {
        if (attribute_2003Parser == null) {
            attribute_2003Parser = createAttribute_2003Parser();
        }
        return attribute_2003Parser;
    }

    /**
	 * @generated
	 */
    protected IParser createAttribute_2003Parser() {
        EAttribute[] features = new EAttribute[] { ModelPackage.eINSTANCE.getAttribute_Name(), ModelPackage.eINSTANCE.getAttribute_Datatype(), ModelPackage.eINSTANCE.getAttribute_PrimaryKey() };
        MessageFormatParser reader = new MessageFormatParser(features);
        reader.setViewPattern("{0} : {1} {2,choice,0#|1#'<<'id'>>'}");
        reader.setEditorPattern("{0} : {1}");
        RegexpParser writer = new RegexpParser(features);
        writer.setEditPattern("{0} *: *{1}");
        return new CompositeParser(reader, writer);
    }

    /**
	 * @generated
	 */
    private IParser compositionAttributeName_4010Parser;

    /**
	 * @generated
	 */
    private IParser getCompositionAttributeName_4010Parser() {
        if (compositionAttributeName_4010Parser == null) {
            compositionAttributeName_4010Parser = createCompositionAttributeName_4010Parser();
        }
        return compositionAttributeName_4010Parser;
    }

    /**
	 * @generated
	 */
    protected IParser createCompositionAttributeName_4010Parser() {
        EAttribute[] features = new EAttribute[] { ModelPackage.eINSTANCE.getComposition_AttributeName() };
        MessageFormatParser parser = new MessageFormatParser(features);
        return parser;
    }

    /**
	 * @generated
	 */
    private IParser compositionPrimaryKey_4011Parser;

    /**
	 * @generated
	 */
    private IParser getCompositionPrimaryKey_4011Parser() {
        if (compositionPrimaryKey_4011Parser == null) {
            compositionPrimaryKey_4011Parser = createCompositionPrimaryKey_4011Parser();
        }
        return compositionPrimaryKey_4011Parser;
    }

    /**
	 * @generated
	 */
    protected IParser createCompositionPrimaryKey_4011Parser() {
        EAttribute[] features = new EAttribute[] { ModelPackage.eINSTANCE.getComposition_PrimaryKey() };
        MessageFormatParser parser = new MessageFormatParser(features);
        parser.setViewPattern("{0,choice,0#|1#'<<'id'>>'}");
        parser.setEditorPattern("{0,choice,0#|1#'<<'id'>>'}");
        parser.setEditPattern("{0,choice,0#|1#'<<'id'>>'}");
        return parser;
    }

    /**
	 * @generated
	 */
    private IParser biOneToOneOwnerAttribute_4012Parser;

    /**
	 * @generated
	 */
    private IParser getBiOneToOneOwnerAttribute_4012Parser() {
        if (biOneToOneOwnerAttribute_4012Parser == null) {
            biOneToOneOwnerAttribute_4012Parser = createBiOneToOneOwnerAttribute_4012Parser();
        }
        return biOneToOneOwnerAttribute_4012Parser;
    }

    /**
	 * @generated
	 */
    protected IParser createBiOneToOneOwnerAttribute_4012Parser() {
        EAttribute[] features = new EAttribute[] { ModelPackage.eINSTANCE.getRelation_OwnerAttribute() };
        MessageFormatParser parser = new MessageFormatParser(features);
        return parser;
    }

    /**
	 * @generated
	 */
    private IParser biOneToOneOwnedAttribute_4013Parser;

    /**
	 * @generated
	 */
    private IParser getBiOneToOneOwnedAttribute_4013Parser() {
        if (biOneToOneOwnedAttribute_4013Parser == null) {
            biOneToOneOwnedAttribute_4013Parser = createBiOneToOneOwnedAttribute_4013Parser();
        }
        return biOneToOneOwnedAttribute_4013Parser;
    }

    /**
	 * @generated
	 */
    protected IParser createBiOneToOneOwnedAttribute_4013Parser() {
        EAttribute[] features = new EAttribute[] { ModelPackage.eINSTANCE.getBidirectionalRelation_OwnedAttribute() };
        MessageFormatParser parser = new MessageFormatParser(features);
        return parser;
    }

    /**
	 * @generated
	 */
    private IParser biOneToManyOwnerAttribute_4016Parser;

    /**
	 * @generated
	 */
    private IParser getBiOneToManyOwnerAttribute_4016Parser() {
        if (biOneToManyOwnerAttribute_4016Parser == null) {
            biOneToManyOwnerAttribute_4016Parser = createBiOneToManyOwnerAttribute_4016Parser();
        }
        return biOneToManyOwnerAttribute_4016Parser;
    }

    /**
	 * @generated
	 */
    protected IParser createBiOneToManyOwnerAttribute_4016Parser() {
        EAttribute[] features = new EAttribute[] { ModelPackage.eINSTANCE.getRelation_OwnerAttribute() };
        MessageFormatParser parser = new MessageFormatParser(features);
        return parser;
    }

    /**
	 * @generated
	 */
    private IParser biOneToManyOwnedAttribute_4017Parser;

    /**
	 * @generated
	 */
    private IParser getBiOneToManyOwnedAttribute_4017Parser() {
        if (biOneToManyOwnedAttribute_4017Parser == null) {
            biOneToManyOwnedAttribute_4017Parser = createBiOneToManyOwnedAttribute_4017Parser();
        }
        return biOneToManyOwnedAttribute_4017Parser;
    }

    /**
	 * @generated
	 */
    protected IParser createBiOneToManyOwnedAttribute_4017Parser() {
        EAttribute[] features = new EAttribute[] { ModelPackage.eINSTANCE.getBidirectionalRelation_OwnedAttribute() };
        MessageFormatParser parser = new MessageFormatParser(features);
        return parser;
    }

    /**
	 * @generated
	 */
    private IParser biManyToManyOwnerAttribute_4020Parser;

    /**
	 * @generated
	 */
    private IParser getBiManyToManyOwnerAttribute_4020Parser() {
        if (biManyToManyOwnerAttribute_4020Parser == null) {
            biManyToManyOwnerAttribute_4020Parser = createBiManyToManyOwnerAttribute_4020Parser();
        }
        return biManyToManyOwnerAttribute_4020Parser;
    }

    /**
	 * @generated
	 */
    protected IParser createBiManyToManyOwnerAttribute_4020Parser() {
        EAttribute[] features = new EAttribute[] { ModelPackage.eINSTANCE.getRelation_OwnerAttribute() };
        MessageFormatParser parser = new MessageFormatParser(features);
        return parser;
    }

    /**
	 * @generated
	 */
    private IParser biManyToManyOwnedAttribute_4021Parser;

    /**
	 * @generated
	 */
    private IParser getBiManyToManyOwnedAttribute_4021Parser() {
        if (biManyToManyOwnedAttribute_4021Parser == null) {
            biManyToManyOwnedAttribute_4021Parser = createBiManyToManyOwnedAttribute_4021Parser();
        }
        return biManyToManyOwnedAttribute_4021Parser;
    }

    /**
	 * @generated
	 */
    protected IParser createBiManyToManyOwnedAttribute_4021Parser() {
        EAttribute[] features = new EAttribute[] { ModelPackage.eINSTANCE.getBidirectionalRelation_OwnedAttribute() };
        MessageFormatParser parser = new MessageFormatParser(features);
        return parser;
    }

    /**
	 * @generated
	 */
    private IParser uniOneToOneOwnerAttribute_4024Parser;

    /**
	 * @generated
	 */
    private IParser getUniOneToOneOwnerAttribute_4024Parser() {
        if (uniOneToOneOwnerAttribute_4024Parser == null) {
            uniOneToOneOwnerAttribute_4024Parser = createUniOneToOneOwnerAttribute_4024Parser();
        }
        return uniOneToOneOwnerAttribute_4024Parser;
    }

    /**
	 * @generated
	 */
    protected IParser createUniOneToOneOwnerAttribute_4024Parser() {
        EAttribute[] features = new EAttribute[] { ModelPackage.eINSTANCE.getRelation_OwnerAttribute() };
        MessageFormatParser parser = new MessageFormatParser(features);
        return parser;
    }

    /**
	 * @generated
	 */
    private IParser uniOneToManyOwnerAttribute_4027Parser;

    /**
	 * @generated
	 */
    private IParser getUniOneToManyOwnerAttribute_4027Parser() {
        if (uniOneToManyOwnerAttribute_4027Parser == null) {
            uniOneToManyOwnerAttribute_4027Parser = createUniOneToManyOwnerAttribute_4027Parser();
        }
        return uniOneToManyOwnerAttribute_4027Parser;
    }

    /**
	 * @generated
	 */
    protected IParser createUniOneToManyOwnerAttribute_4027Parser() {
        EAttribute[] features = new EAttribute[] { ModelPackage.eINSTANCE.getRelation_OwnerAttribute() };
        MessageFormatParser parser = new MessageFormatParser(features);
        return parser;
    }

    /**
	 * @generated
	 */
    private IParser uniManyToManyOwnerAttribute_4030Parser;

    /**
	 * @generated
	 */
    private IParser getUniManyToManyOwnerAttribute_4030Parser() {
        if (uniManyToManyOwnerAttribute_4030Parser == null) {
            uniManyToManyOwnerAttribute_4030Parser = createUniManyToManyOwnerAttribute_4030Parser();
        }
        return uniManyToManyOwnerAttribute_4030Parser;
    }

    /**
	 * @generated
	 */
    protected IParser createUniManyToManyOwnerAttribute_4030Parser() {
        EAttribute[] features = new EAttribute[] { ModelPackage.eINSTANCE.getRelation_OwnerAttribute() };
        MessageFormatParser parser = new MessageFormatParser(features);
        return parser;
    }

    /**
	 * @generated
	 */
    protected IParser getParser(int visualID) {
        switch(visualID) {
            case EntityNameEditPart.VISUAL_ID:
                return getEntityName_4001Parser();
            case EmbeddedNameEditPart.VISUAL_ID:
                return getEmbeddedName_4003Parser();
            case TableGeneratorTableNameEditPart.VISUAL_ID:
                return getTableGeneratorTableName_4006Parser();
            case SequenceGeneratorSequenceNameEditPart.VISUAL_ID:
                return getSequenceGeneratorSequenceName_4008Parser();
            case AttributeEditPart.VISUAL_ID:
                return getAttribute_2001Parser();
            case VersionAttributeEditPart.VISUAL_ID:
                return getVersionAttribute_2002Parser();
            case Attribute2EditPart.VISUAL_ID:
                return getAttribute_2003Parser();
            case CompositionAttributeNameEditPart.VISUAL_ID:
                return getCompositionAttributeName_4010Parser();
            case CompositionPrimaryKeyEditPart.VISUAL_ID:
                return getCompositionPrimaryKey_4011Parser();
            case BiOneToOneOwnerAttributeEditPart.VISUAL_ID:
                return getBiOneToOneOwnerAttribute_4012Parser();
            case BiOneToOneOwnedAttributeEditPart.VISUAL_ID:
                return getBiOneToOneOwnedAttribute_4013Parser();
            case BiOneToManyOwnerAttributeEditPart.VISUAL_ID:
                return getBiOneToManyOwnerAttribute_4016Parser();
            case BiOneToManyOwnedAttributeEditPart.VISUAL_ID:
                return getBiOneToManyOwnedAttribute_4017Parser();
            case BiManyToManyOwnerAttributeEditPart.VISUAL_ID:
                return getBiManyToManyOwnerAttribute_4020Parser();
            case BiManyToManyOwnedAttributeEditPart.VISUAL_ID:
                return getBiManyToManyOwnedAttribute_4021Parser();
            case UniOneToOneOwnerAttributeEditPart.VISUAL_ID:
                return getUniOneToOneOwnerAttribute_4024Parser();
            case UniOneToManyOwnerAttributeEditPart.VISUAL_ID:
                return getUniOneToManyOwnerAttribute_4027Parser();
            case UniManyToManyOwnerAttributeEditPart.VISUAL_ID:
                return getUniManyToManyOwnerAttribute_4030Parser();
        }
        return null;
    }

    /**
	 * @generated
	 */
    public IParser getParser(IAdaptable hint) {
        String vid = (String) hint.getAdapter(String.class);
        if (vid != null) {
            return getParser(OrmmetaVisualIDRegistry.getVisualID(vid));
        }
        View view = (View) hint.getAdapter(View.class);
        if (view != null) {
            return getParser(OrmmetaVisualIDRegistry.getVisualID(view));
        }
        return null;
    }

    /**
	 * @generated
	 */
    public boolean provides(IOperation operation) {
        if (operation instanceof GetParserOperation) {
            IAdaptable hint = ((GetParserOperation) operation).getHint();
            if (OrmmetaElementTypes.getElement(hint) == null) {
                return false;
            }
            return getParser(hint) != null;
        }
        return false;
    }

    /**
	 * @generated
	 */
    public static class HintAdapter extends ParserHintAdapter {

        /**
		 * @generated
		 */
        private final IElementType elementType;

        /**
		 * @generated
		 */
        public HintAdapter(IElementType type, EObject object, String parserHint) {
            super(object, parserHint);
            assert type != null;
            elementType = type;
        }

        /**
		 * @generated
		 */
        public Object getAdapter(Class adapter) {
            if (IElementType.class.equals(adapter)) {
                return elementType;
            }
            return super.getAdapter(adapter);
        }
    }
}
