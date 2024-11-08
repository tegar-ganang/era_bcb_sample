package academico.persistence;

import academico.model.Aluno;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AlunoDAOArquivo implements AlunoDAO {

    private static RandomAccessFile file;

    public AlunoDAOArquivo() {
        super();
        verificaArquivo();
    }

    public static Aluno getProximo(boolean start) {
        String cname = "Aluno";
        Aluno alunoaux = null;
        try {
            if (start) {
                file = new RandomAccessFile(cname + ".dat", "r");
                file.readInt();
            }
            int idaux;
            String nome;
            String sobrenome;
            do {
                idaux = file.readInt();
                nome = file.readUTF();
                sobrenome = file.readUTF();
                alunoaux = new Aluno();
                alunoaux.setId(idaux);
                alunoaux.setNome(nome);
                alunoaux.setSobrenome(sobrenome);
            } while (idaux == 0);
            return alunoaux;
        } catch (IOException e) {
            try {
                if (file != null) {
                    file.close();
                    file = null;
                }
            } catch (IOException e2) {
                System.err.println(e2.getMessage());
            }
        }
        return null;
    }

    public static void clean() {
        try {
            RandomAccessFile in = new RandomAccessFile("Aluno.dat", "rw");
            RandomAccessFile out = new RandomAccessFile("Aluno.tmp", "rw");
            int high = in.readInt();
            out.writeInt(high);
            int idaux;
            String nome;
            String sobrenome;
            while (in.getFilePointer() < in.length()) {
                idaux = in.readInt();
                nome = in.readUTF();
                sobrenome = in.readUTF();
                if (idaux != 0) {
                    out.writeInt(idaux);
                    out.writeUTF(nome);
                    out.writeUTF(sobrenome);
                }
            }
            in.close();
            out.seek(0);
            int ch;
            FileOutputStream dat = new FileOutputStream("Aluno.dat");
            while ((ch = out.read()) != -1) dat.write(ch);
            dat.close();
            out.close();
            File newf = new File("Aluno.tmp");
            newf.delete();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private int verificaArquivo() {
        int id = 0;
        try {
            RandomAccessFile file = new RandomAccessFile("Aluno.dat", "rw");
            try {
                id = file.readInt();
                file.seek(0);
                id = id + 1;
                file.writeInt(id);
            } catch (IOException e) {
                id = 1;
                try {
                    file.writeInt(id);
                } catch (IOException e2) {
                    System.err.println(e2.getMessage());
                }
            }
            if (file != null) file.close();
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
        return id;
    }

    public boolean armazenar(Aluno aluno) {
        boolean ret = false;
        int id = 0;
        try {
            RandomAccessFile out = new RandomAccessFile("Aluno.dat", "rw");
            out.seek(out.length());
            if (aluno.getId() != 0) {
                id = aluno.getId();
            } else {
                id = verificaArquivo();
            }
            out.writeInt(id);
            out.writeUTF(aluno.getNome());
            out.writeUTF(aluno.getSobrenome());
            out.close();
            ret = true;
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return ret;
    }

    public Aluno recuperar(int idref) {
        try {
            RandomAccessFile in = new RandomAccessFile("Aluno.dat", "r");
            in.readInt();
            int idaux;
            String nome;
            String sobrenome;
            while (in.getFilePointer() < in.length()) {
                idaux = in.readInt();
                nome = in.readUTF();
                sobrenome = in.readUTF();
                if (idaux == idref) {
                    in.close();
                    Aluno novoAluno = new Aluno();
                    novoAluno.setId(idaux);
                    novoAluno.setNome(nome);
                    novoAluno.setSobrenome(sobrenome);
                    return novoAluno;
                }
            }
            in.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    public boolean atualizar(Aluno aluno) {
        boolean retorno = false;
        retorno = remover(aluno.getId());
        if (retorno) retorno = armazenar(aluno);
        return retorno;
    }

    public boolean remover(int id) {
        try {
            RandomAccessFile in = new RandomAccessFile("Aluno.dat", "rw");
            in.readInt();
            long filepointer = in.getFilePointer();
            int idaux = in.readInt();
            in.readUTF();
            in.readUTF();
            do {
                if (idaux == id) {
                    in.seek(filepointer);
                    in.writeInt(0);
                    in.close();
                    return true;
                } else {
                    filepointer = in.getFilePointer();
                    idaux = in.readInt();
                    in.readUTF();
                    in.readUTF();
                }
            } while (true);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return false;
    }

    public List<Aluno> listar() {
        List<Aluno> alunos = new ArrayList<Aluno>();
        Aluno aluno = null;
        int idaux;
        String nome;
        String sobrenome;
        try {
            RandomAccessFile in = new RandomAccessFile("Aluno.dat", "r");
            in.readInt();
            while (in.getFilePointer() < in.length()) {
                idaux = in.readInt();
                nome = in.readUTF();
                sobrenome = in.readUTF();
                if (idaux != 0) {
                    aluno = new Aluno();
                    aluno.setId(idaux);
                    aluno.setNome(nome);
                    aluno.setSobrenome(sobrenome);
                    alunos.add(aluno);
                }
            }
            in.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return alunos;
    }
}
