require 'open3'
require 'tmpdir'
require 'rbconfig'
require 'yaml'
require 'rake'


module TorqueBox
  module RakeUtils
    def self.jboss_home
      jboss_home = File.expand_path(ENV['JBOSS_HOME']) if ENV['JBOSS_HOME']
      jboss_home ||= File.join(File.expand_path(ENV['TORQUEBOX_HOME']), "jboss") if ENV['TORQUEBOX_HOME']
      raise "$JBOSS_HOME is not set" unless jboss_home
      return jboss_home
    end
    def self.jboss_conf
      ENV['TORQUEBOX_CONF'] || ENV['JBOSS_CONF'] || 'default'
    end
    def self.server_dir
      File.join("#{jboss_home}","server", "#{jboss_conf}" )
    end
    def self.deploy_dir
      File.join("#{server_dir}","deploy")
    end
    def self.deployers_dir
      File.join("#{server_dir}","deployers")
    end
    def self.command_line
      cmd = Config::CONFIG['host_os'] =~ /mswin/ ? "bin\\run" : "/bin/sh bin/run.sh"
      options = ENV['JBOSS_OPTS']
      cmd += " -b 0.0.0.0" unless /((^|\s)-b\s|(^|\s)--host=)/ =~ options
      "#{cmd} -c #{jboss_conf} #{options}"
    end
    def self.run_server()
      Dir.chdir(jboss_home) do
        old_trap = trap("INT") do
          puts "caught SIGINT, shutting down"
        end
        pid = Open3.popen3( command_line ) do |stdin, stdout, stderr|
          #stdin.close
          threads = []
          threads << Thread.new(stdout) do |input_str|
            while ( ( l = input_str.gets ) != nil )
              puts l
            end
          end
          threads << Thread.new(stderr) do |input_str|
            while ( ( l = input_str.gets ) != nil )
              puts l
            end
          end
          threads.each{|t|t.join}
        end
        trap("INT", old_trap )
      end
    end
    def self.deploy_yaml(deployment_name, deployment_descriptor)
      deployment = File.join( deploy_dir, deployment_name )
      File.open( deployment, 'w' ) do |file|
        YAML.dump( deployment_descriptor, file )
      end
    end

    def self.undeploy(deployment_name)
       deployment = File.join( deploy_dir, deployment_name )
       FileUtils.rm_rf( deployment )
    end
  end
end

