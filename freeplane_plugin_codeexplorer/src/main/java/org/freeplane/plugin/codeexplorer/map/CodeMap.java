package org.freeplane.plugin.codeexplorer.map;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.freeplane.features.map.INodeDuplicator;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.NodeRelativePath;
import org.freeplane.features.map.mindmapmode.MMapModel;
import org.freeplane.features.styles.MapStyleModel;
import org.freeplane.plugin.codeexplorer.dependencies.CodeDependency;
import org.freeplane.plugin.codeexplorer.dependencies.DependencyVerdict;
import org.freeplane.plugin.codeexplorer.task.AnnotationMatcher;
import org.freeplane.plugin.codeexplorer.task.DependencyJudge;
import org.freeplane.plugin.codeexplorer.task.DependencyRuleJudge;
import org.freeplane.plugin.codeexplorer.task.CodeExplorerConfiguration;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;

public class CodeMap extends MMapModel {

    private DependencyJudge judge = new DependencyRuleJudge();
    private SubprojectFinder subprojectFinder = SubprojectFinder.EMPTY;
    private CodeExplorerConfiguration codeExplorerConfiguration;
    private boolean canBeSaved = false;

    public CodeMap(INodeDuplicator nodeDuplicator) {
        super(nodeDuplicator);
        setRoot(new EmptyNodeModel(this, "No locations selected"));
        getRootNode().setFolded(false);
    }

    @SuppressWarnings("serial")
    @Override
    protected Map<String, NodeModel> createNodeByIdMap() {
        return new ConcurrentHashMap<String, NodeModel>() {

            @Override
            public NodeModel put(String key, NodeModel value) {
                if(value == null)
                    return remove(key);
                else
                    return super.put(key, value);
            }

        };
    }

    @Override
    public boolean isSaved() {
         return ! canBeSaved || super.isSaved();
    }

    @Override
    public String getTitle() {
        final String longTitle = "Code: " + getRootNode().toString();
        final int maximumTitleLEngth = 30;
        return longTitle.length() <= maximumTitleLEngth ? longTitle : longTitle.substring(0, maximumTitleLEngth - 3 ) + "...";
    }

    public void setJudge(DependencyJudge judge) {
        this.judge = judge;
    }

    @Override
    public void enableAutosave() {/**/}


    public void updateAnnotations(AnnotationMatcher annotationMatcher) {
        boolean saved = isSaved();
        getRootNode().updateAnnotations(annotationMatcher);
        if(saved)
            setSaved(true);
    }

    public DependencyJudge getJudge() {
        return judge;
    }



    @Override
    public CodeNode getRootNode() {
         return (CodeNode) super.getRootNode();
    }

    @Override
    public void setRoot(NodeModel newRoot) {
        NodeModel oldRoot = getRootNode();
        if(oldRoot != null) {
            MapStyleModel mapStyles = oldRoot.getExtension(MapStyleModel.class);
            if(mapStyles != null)
                newRoot.addExtension(mapStyles);
        }
        super.setRoot(newRoot);
    }


    public void setSubprojectFinder(SubprojectFinder subprojectFinder) {
        this.subprojectFinder = subprojectFinder;
    }

    DependencyVerdict judge(Dependency dependency, boolean goesUp) {
        return judge.judge(dependency, goesUp);
    }

    public int subprojectIndexOf(JavaClass javaClass) {
        return subprojectFinder.subprojectIndexOf(javaClass);
    }

    public int subprojectIndexOf(String location) {
        return subprojectFinder.subprojectIndexOf(location);
    }



    public Stream<JavaClass> allClasses() {
        return subprojectFinder.allClasses();
    }


    public String getClassNodeId(JavaClass javaClass) {
        int subprojectIndex = subprojectIndexOf(javaClass);
        return getClassNodeId(javaClass, subprojectIndex);
    }

    private String getClassNodeId(JavaClass javaClass, int subprojectIndex) {
        JavaClass nodeClass = CodeNode.findEnclosingNamedClass(javaClass);
        String nodeClassName = nodeClass.getName();
        String classNodeId = CodeNode.idWithSubprojectIndex(nodeClassName, subprojectIndex);
        return classNodeId;
    }

   public CodeNode getNodeByClass(JavaClass javaClass) {
        String existingNodeId = getExistingNodeId(javaClass);
        return existingNodeId == null ? null : (CodeNode) getNodeForID(existingNodeId);
    }

    String getExistingNodeId(JavaClass javaClass) {
        int subprojectIndex = subprojectIndexOf(javaClass);
        return getClassNodeId(javaClass, subprojectIndex);
    }

    public CodeDependency toCodeDependency(Dependency dependency) {
        NodeModel originNode = getNodeByClass(dependency.getOriginClass());
        NodeModel targetNode = getNodeByClass(dependency.getTargetClass());
        if(originNode == null || targetNode == null) {
            throw new IllegalStateException("Can not find nodes for dependency " + dependency);
        }
        NodeRelativePath nodeRelativePath = new NodeRelativePath(originNode, targetNode);
        boolean goesUp = nodeRelativePath.compareNodePositions() > 0;
        return new CodeDependency(dependency, goesUp, judge(dependency, goesUp));
    }

    @Override
    public void releaseResources() {
         super.releaseResources();
    }

    public void setConfiguration(CodeExplorerConfiguration codeExplorerConfiguration) {
        this.codeExplorerConfiguration = codeExplorerConfiguration;
    }

    public CodeExplorerConfiguration getConfiguration() {
        return codeExplorerConfiguration;
    }

    public String locationByIndex(int index) {
        return subprojectFinder.locationByIndex(index);
    }

    public void setCanBeSaved(boolean canBeSaved) {
       this.canBeSaved = canBeSaved;
    }

}
