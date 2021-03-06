package org.nutz.dao.impl.entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.nutz.dao.DB;
import org.nutz.dao.DaoException;
import org.nutz.dao.entity.Entity;
import org.nutz.dao.entity.EntityField;
import org.nutz.dao.entity.EntityMaker;
import org.nutz.dao.entity.MappingField;
import org.nutz.dao.entity.annotation.ColType;
import org.nutz.dao.entity.annotation.Column;
import org.nutz.dao.entity.annotation.Comment;
import org.nutz.dao.entity.annotation.EL;
import org.nutz.dao.entity.annotation.Id;
import org.nutz.dao.entity.annotation.Index;
import org.nutz.dao.entity.annotation.Many;
import org.nutz.dao.entity.annotation.ManyMany;
import org.nutz.dao.entity.annotation.Name;
import org.nutz.dao.entity.annotation.One;
import org.nutz.dao.entity.annotation.PK;
import org.nutz.dao.entity.annotation.SQL;
import org.nutz.dao.entity.annotation.Table;
import org.nutz.dao.entity.annotation.TableIndexes;
import org.nutz.dao.entity.annotation.TableMeta;
import org.nutz.dao.entity.annotation.View;
import org.nutz.dao.impl.EntityHolder;
import org.nutz.dao.impl.entity.field.ManyLinkField;
import org.nutz.dao.impl.entity.field.ManyManyLinkField;
import org.nutz.dao.impl.entity.field.NutMappingField;
import org.nutz.dao.impl.entity.field.OneLinkField;
import org.nutz.dao.impl.entity.info.LinkInfo;
import org.nutz.dao.impl.entity.info.MappingInfo;
import org.nutz.dao.impl.entity.info.TableInfo;
import org.nutz.dao.impl.entity.info._Infos;
import org.nutz.dao.impl.entity.macro.ElFieldMacro;
import org.nutz.dao.impl.entity.macro.SqlFieldMacro;
import org.nutz.dao.jdbc.JdbcExpert;
import org.nutz.dao.jdbc.Jdbcs;
import org.nutz.dao.sql.Pojo;
import org.nutz.dao.util.Daos;
import org.nutz.lang.Lang;
import org.nutz.lang.Mirror;
import org.nutz.lang.Strings;
import org.nutz.lang.segment.CharSegment;
import org.nutz.log.Log;
import org.nutz.log.Logs;
import org.nutz.trans.Trans;

/**
 * ???????????? Class ???????????? Entity ?????????
 *
 * @author zozoh(zozohtnt@gmail.com)
 */
public class AnnotationEntityMaker implements EntityMaker {

    private static final Log log = Logs.get();

    private DataSource datasource;

    private JdbcExpert expert;

    private EntityHolder holder;

    protected AnnotationEntityMaker() {
	}

    public void init(DataSource datasource, JdbcExpert expert, EntityHolder holder) {
    	this.datasource = datasource;
        this.expert = expert;
        this.holder = holder;
    }

    public AnnotationEntityMaker(DataSource datasource, JdbcExpert expert, EntityHolder holder) {
        init(datasource, expert, holder);
    }

    public <T> Entity<T> make(Class<T> type) {
        NutEntity<T> en = _createNutEntity(type);

        TableInfo ti = _createTableInfo(type);

        /*
         * ???????????????????????????
         */
        // ??????
        if (null != expert.getConf()) {
            for (String key : expert.getConf().keySet())
                en.getMetas().put(key, expert.getConf().get(key));
        }
        // ?????????
        if (null != ti.annMeta) {
            Map<String, Object> map = Lang.map(ti.annMeta.value());
            for (Entry<String, Object> entry : map.entrySet()) {
                en.getMetas().put(entry.getKey(), entry.getValue().toString());
            }
        }

        /*
         * ???????????????????????????????????????
         */
        String tableName = null;
        if (null == ti.annTable) {
        	tableName = Daos.getTableNameMaker().make(type);
        	if (null == ti.annView)
        	    log.warnf("No @Table found, fallback to use table name='%s' for type '%s'", tableName, type.getName());
        } else {
        	    tableName = ti.annTable.value().isEmpty() ? Daos.getTableNameMaker().make(type) : ti.annTable.value();
            if (!ti.annTable.prefix().isEmpty()) {
                tableName = ti.annTable.prefix() + tableName;
            }
            if (!ti.annTable.suffix().isEmpty()) {
                tableName = tableName + ti.annTable.suffix();
            }
        }

        String viewName = null;
        if (null == ti.annView) {
            viewName = tableName;
        } else {
            viewName = ti.annView.value().isEmpty() ? Daos.getViewNameMaker().make(type) : ti.annView.value();
            if (!ti.annView.prefix().isEmpty()) {
                viewName = ti.annView.prefix() + viewName;
            }
            if (!ti.annView.suffix().isEmpty()) {
                viewName = viewName + ti.annView.suffix();
            }
        }

        en.setTableName(tableName);
        en.setViewName(viewName);

        boolean hasTableComment = null != ti.tableComment;
        String tableComment = hasTableComment ? Strings.isBlank(ti.tableComment.value()) ? type.getName()
                                                                                        : ti.tableComment.value()
                                             : null;
        en.setHasTableComment(hasTableComment);
        en.setTableComment(tableComment);

        /*
         * ??????????????????????????????
         */
        // ????????????????????????????????? '@Column' @Comment
        boolean shouldUseColumn = false;
        boolean hasColumnComment = false;
        for (Field field : en.getMirror().getFields()) {
            if (shouldUseColumn && hasColumnComment) {
                break;
            }
            if (!shouldUseColumn && null != field.getAnnotation(Column.class)) {
                shouldUseColumn = true;
            }
            if (!hasColumnComment && null != field.getAnnotation(Comment.class)) {
                hasColumnComment = true;
            }
        }

        en.setHasColumnComment(hasColumnComment);

        /*
         * ????????????????????????
         */
        List<MappingInfo> infos = new ArrayList<MappingInfo>();
        List<LinkInfo> ones = new ArrayList<LinkInfo>();
        List<LinkInfo> manys = new ArrayList<LinkInfo>();
        List<LinkInfo> manymanys = new ArrayList<LinkInfo>();

        String[] _tmp = ti.annPK == null ? null : ti.annPK.value();
        List<String> pks = _tmp == null ? new ArrayList<String>() : Arrays.asList(_tmp);
        // ????????????????????????????????????????????????????????????
        for (Field field : en.getMirror().getFields()) {
            // '@One'
            if (null != field.getAnnotation(One.class)) {
                ones.add(_Infos.createLinkInfo(field));
            }
            // '@Many'
            else if (null != field.getAnnotation(Many.class)) {
                manys.add(_Infos.createLinkInfo(field));
            }
            // '@ManyMany'
            else if (null != field.getAnnotation(ManyMany.class)) {
                manymanys.add(_Infos.createLinkInfo(field));
            }
            // ????????????
            else if ((Modifier.isTransient(field.getModifiers()) && null == field.getAnnotation(Column.class))
                     || (shouldUseColumn && (null == field.getAnnotation(Column.class)
                                             && null == field.getAnnotation(Id.class) && null == field.getAnnotation(Name.class)))
                                             && !pks.contains(field.getName())) {
                continue;
            }
            // '@Column'
            else {
                infos.add(_Infos.createMappingInfo(ti.annPK, field));
            }

        }
        // ???????????????????????????????????????????????????????????????
        for (Method method : en.getType().getMethods()) {
            // '@One'
            if (null != method.getAnnotation(One.class)) {
                ones.add(_Infos.createLinkInfo(method));
            }
            // '@Many'
            else if (null != method.getAnnotation(Many.class)) {
                manys.add(_Infos.createLinkInfo(method));
            }
            // '@ManyMany'
            else if (null != method.getAnnotation(ManyMany.class)) {
                manymanys.add(_Infos.createLinkInfo(method));
            }
            // ????????????
            else if (null == method.getAnnotation(Column.class)
                     && null == method.getAnnotation(Id.class)
                     && null == method.getAnnotation(Name.class)) {
                continue;
            }
            // '@Column'
            else {
                infos.add(_Infos.createMapingInfo(ti.annPK, method));
            }
        }

        // ?????????????????????, fix issue #29
        List<MappingInfo> tmp = new ArrayList<MappingInfo>(infos.size());
        MappingInfo miId = null;
        MappingInfo miName = null;
        MappingInfo miVersion = null;//wjw(2017-04-10),add,version
        for (MappingInfo mi : infos) {
            if (mi.annId != null) {
            	if (miId != null) {
            		throw new DaoException("Allows only a single @Id ! " + type);
            	}
                miId = mi;
            }
            else if (mi.annName != null) {
            	if (miName != null) {
            		throw new DaoException("Allows only a single @Name ! " + type);
            	}
                miName = mi;
            }
            else{
            	//wjw(2017-04-10),add,version
           	    if(mi.annColumn != null && mi.annColumn.version()){
                    if(miVersion != null){
                        throw new DaoException("Allows only a single @Version ! " + type);
                	}
                	miVersion = mi;
                }
            	tmp.add(mi);
            }
        }
        if (miName != null)
            tmp.add(0, miName);
        if (miId != null)
            tmp.add(0, miId);
        infos = tmp;

        // ?????????????????????? ?????????????????????????????????!!
        if (infos.isEmpty())
            throw Lang.makeThrow(IllegalArgumentException.class,
                                 "Pojo(%s) without any Mapping Field!!",
                                 type);

        /*
         * ????????????????????????
         */
        for (MappingInfo info : infos) {
            NutMappingField ef = new NutMappingField(en);
            _evalMappingField(ef, info);
            en.addMappingField(ef);
        }
        holder.set(en); // ???????????????????????????????????????????????????????????????
        try {
			/*
			 * ????????????????????????
			 */
			// ????????? '@One'
			for (LinkInfo li : ones) {
			    en.addLinkField(new OneLinkField(en, holder, li));
			}
			// ????????? '@Many'
			for (LinkInfo li : manys) {
			    en.addLinkField(new ManyLinkField(en, holder, li));
			}
			// ????????? '@ManyMany'
			for (LinkInfo li : manymanys) {
			    en.addLinkField(new ManyManyLinkField(en, holder, li));
			}
			// ??????????????????
			en.checkCompositeFields(null == ti.annPK ? null : ti.annPK.value());

			/*
			 * ????????? expert ?????????????????????????????????
			 */
			if (null != datasource && null != expert) {
			    _checkupEntityFieldsWithDatabase(en);
			}

			/*
			 * ???????????????
			 */
			_evalFieldMacro(en, infos);

			/*
			 * ??????????????????
			 */
			if (null != ti.annIndexes)
			    _evalEntityIndexes(en, ti.annIndexes);
		} catch (RuntimeException e) {
			holder.remove(en);
			throw e;
		} catch (Throwable e) {
			holder.remove(en);
			throw Lang.wrapThrow(e);
		}

        // ????????????????????? ^_^
        en.setComplete(true);
        return en;
    }

    /**
     * ????????????????????????????????????
     *
     * @param type
     *            ????????????
     * @return ???????????????
     */
    private TableInfo _createTableInfo(Class<?> type) {
        TableInfo info = new TableInfo();
        Mirror<?> mirror = Mirror.me(type);
        info.annTable = mirror.getAnnotation(Table.class);
        info.annView = mirror.getAnnotation(View.class);
        info.annMeta = mirror.getAnnotation(TableMeta.class);
        info.annPK = mirror.getAnnotation(PK.class);
        info.annIndexes = mirror.getAnnotation(TableIndexes.class);
        info.tableComment = mirror.getAnnotation(Comment.class);
        return info;
    }

    /**
     * ?????? '@Next' ??? '@Prev' ???????????????????????? FieldMacroInfo ??????
     *
     * @param els
     *            ?????????
     * @param sqls
     *            SQL
     * @return ??????????????????????????????
     */
    private List<FieldMacroInfo> _annToFieldMacroInfo(EL[] els, SQL[] sqls) {
        List<FieldMacroInfo> mis = new LinkedList<FieldMacroInfo>();
        if (els.length > 0) { // els ??????????????? null ???
            for (EL el : els)
                mis.add(new FieldMacroInfo(el));
        }
        if (sqls.length > 0) { // @SQL ?????? @EL ????????????
            for (SQL sql : sqls)
                mis.add(new FieldMacroInfo(sql));
        }
        return mis;
    }

    /**
     * @param ef
     * @param info
     */
    private void _evalMappingField(NutMappingField ef, MappingInfo info) {
        // ????????? Java ??????
        ef.setName(info.name);
        ef.setType(info.fieldType);
        String columnName = "";
        // ?????????????????????
        if (null == info.annColumn || Strings.isBlank(info.annColumn.value())){
            columnName = info.name;
            if((null != info.annColumn && info.annColumn.hump()) || Daos.FORCE_HUMP_COLUMN_NAME){
                columnName = Strings.lowerWord(columnName, '_');
            }
        }else{
            columnName = info.annColumn.value();
        }

        if (null != info.annColumn) {
            if (!info.annColumn.prefix().isEmpty()) {
                columnName = info.annColumn.prefix() + columnName;
            }
            if (!info.annColumn.suffix().isEmpty()) {
                columnName = columnName + info.annColumn.suffix();
            }
        }

        ef.setColumnName(columnName);
        // ???????????????
        boolean hasColumnComment = null != info.columnComment;
        ef.setHasColumnComment(hasColumnComment);
        if (hasColumnComment) {
            String comment = info.columnComment.value();
            if (Strings.isBlank(comment)) {
                ef.setColumnComment(info.name);
            } else {
                ef.setColumnComment(comment);
            }
        }

        //wjw(2017-04-10),add,version
        if(null != info.annColumn && info.annColumn.version()){
        	ef.setAsVersion();
        }

        // Id ??????
        if (null != info.annId) {
            ef.setAsId();
            if (info.annId.auto()) {
                ef.setAsAutoIncreasement();
            }
        }

        // Name ??????
        if (null != info.annName) {
            ef.setAsName();
            ef.setCasesensitive(info.annName.casesensitive());
        }

        // ?????? @Id ??? @Name ?????????
        if (ef.isId() && ef.isName())
            throw Lang.makeThrow("Field '%s'(%s) can not be @Id and @Name at same time!",
                                 ef.getName(),
                                 ef.getEntity().getType().getName());

        // ?????? PK
        if (null != info.annPK) {
            // ??? @PK ????????????????????????
            if (info.annPK.value().length == 1) {
                if (Lang.contains(info.annPK.value(), info.name)) {
                    if (ef.getTypeMirror().isIntLike())
                        ef.setAsId();
                    else
                        ef.setAsName();
                }
            }
            // ???????????????????????????
            else if (Lang.contains(info.annPK.value(), info.name))
                ef.setAsCompositePk();
        }

        // ?????????
        if (null != info.annDefault)
            ef.setDefaultValue(new CharSegment(info.annDefault.value()));

        // ??????
        if (null != info.annReadonly)
            ef.setAsReadonly();

        // ??????????????????
        if (null != info.annDefine) {
            // ??????
            if (info.annDefine.type() != ColType.AUTO)
                ef.setColumnType(info.annDefine.type());
            else
                Jdbcs.guessEntityFieldColumnType(ef);
            // ??????
            ef.setWidth(info.annDefine.width());
            if (ef.getWidth() == 0 && ef.getColumnType() == ColType.VARCHAR) {
            	ef.setWidth(Daos.DEFAULT_VARCHAR_WIDTH);
            }
            // ??????
            ef.setPrecision(info.annDefine.precision());
            // ?????????
            if (info.annDefine.unsigned())
                ef.setAsUnsigned();
            // ????????????
            if (info.annDefine.notNull())
                ef.setAsNotNull();
            // ??????????????? @Id(auto=false)????????????
            if (info.annDefine.auto() && !ef.isId())
                ef.setAsAutoIncreasement();

            // ????????????????????????????
            if (info.annDefine.customType().length() > 0) {
                ef.setCustomDbType(info.annDefine.customType());
            }

            // ??????????????????
            ef.setInsert(info.annDefine.insert());
            ef.setUpdate(info.annDefine.update());
        }
        // ??????????????????
        if (ef.getColumnType() == null) {
            Jdbcs.guessEntityFieldColumnType(ef);
        }

        // ?????????????????????
        if (null == info.annDefine || null == info.annDefine.adaptor() || info.annDefine.adaptor().isInterface())
            ef.setAdaptor(expert.getAdaptor(ef));
        else
            ef.setAdaptor(Mirror.me(info.annDefine.adaptor()).born());

        // ????????????
        ef.setInjecting(info.injecting);
        ef.setEjecting(info.ejecting);

        // ?????????????
        if (Daos.FORCE_UPPER_COLUMN_NAME) {
            ef.setColumnName(ef.getColumnName().toUpperCase());
        }
        if (Daos.FORCE_WRAP_COLUMN_NAME || (info.annColumn != null && info.annColumn.wrap())) {
            ef.setColumnNameInSql(expert.wrapKeywork(columnName, true));
        }
        else if (Daos.CHECK_COLUMN_NAME_KEYWORD) {
            ef.setColumnNameInSql(expert.wrapKeywork(columnName, false));
        }
    }

    private void _evalFieldMacro(Entity<?> en, List<MappingInfo> infos) {
        for (MappingInfo info : infos) {
            // '@Prev' : ?????????
            if (null != info.annPrev) {
                boolean flag = en.addBeforeInsertMacro(__macro(en.getField(info.name),
                                                _annToFieldMacroInfo(info.annPrev.els(),
                                                                     info.annPrev.value())));
                if (flag && null != info.annId && info.annId.auto()) {
                    log.debugf("Field(%s#%s) autoset as @Id(auto=false)", en.getType().getName(), info.name);
                    ((NutMappingField)en.getField(info.name)).setAutoIncreasement(false);
                }
            }

            // '@Next' : ????????????
            if (null != info.annNext
                && en.addAfterInsertMacro(__macro(en.getField(info.name),
                                                  _annToFieldMacroInfo(info.annNext.els(),
                                                                       info.annNext.value())))) {
                continue;
            }
            // '@Id' : ?????????????????????
            else if (null != info.annId && info.annId.auto() && en.getField(info.name).isAutoIncreasement()) {
            	if (!expert.isSupportAutoIncrement() || !expert.isSupportGeneratedKeys())
            		en.addAfterInsertMacro(expert.fetchPojoId(en, en.getField(info.name)));
            }
        }
    }

    private Pojo __macro(MappingField ef, List<FieldMacroInfo> infoList) {
        FieldMacroInfo theInfo = null;
        // ??????????????????????????????????????????
        for (FieldMacroInfo info : infoList) {
            if (DB.OTHER == info.getDb()) {
                theInfo = info;
            } else if (info.getDb().name().equalsIgnoreCase(expert.getDatabaseType())) {
                theInfo = info;
                break;
            }
        }
        // ?????????????????????
        if (null != theInfo) {
            if (theInfo.isEl())
                return new ElFieldMacro(ef, theInfo.getValue());
            else
                return new SqlFieldMacro(ef, theInfo.getValue());
        }
        return null;
    }

    private void _evalEntityIndexes(NutEntity<?> en, TableIndexes indexes) {
        for (Index idx : indexes.value()) {
            NutEntityIndex index = new NutEntityIndex();
            index.setUnique(idx.unique());
            index.setName(idx.name());
            for (String indexName : idx.fields()) {
                EntityField ef = en.getField(indexName);
                if (null == ef) {
                    throw Lang.makeThrow("Fail to find field '%s' in '%s' by @Index(%s:%s)",
                                         indexName,
                                         en.getType().getName(),
                                         index.getName(en),
                                         Lang.concat(idx.fields()));
                }
                index.addField(ef);
            }
            en.addIndex(index);
        }
        for (Field field : en.getMirror().getFields()) {
            Index idx = field.getAnnotation(Index.class);
            if (idx == null)
                continue;
            NutEntityIndex index = new NutEntityIndex();
            index.setUnique(idx.unique());
            index.setName(idx.name());
            index.addField(en.getField(field.getName()));
            en.addIndex(index);
        }
    }

    private void _checkupEntityFieldsWithDatabase(NutEntity<?> en) {
        Connection conn = null;
        try {
            conn = Trans.getConnectionAuto(datasource);
            expert.setupEntityField(conn, en);
        }
        catch (Exception e) {
            if (log.isDebugEnabled())
                log.debugf("Fail to setup '%s'(%s) by DB, because: (%s)'%s'",
                           en.getType().getName(),
                           en.getTableName(),
                           e.getClass().getName(),
                           e.getMessage());
        }
        finally {
            Trans.closeConnectionAuto(conn);
        }
    }

    protected <T> NutEntity<T> _createNutEntity(Class<T> type) {
        return new NutEntity<T>(type);
    }
}
